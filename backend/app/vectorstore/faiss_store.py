import faiss
import numpy as np
import json
from pathlib import Path


class FAISSVectorStore:

    def __init__(
            self,
            dimension: int = 384,
            index_path: str = "data/faiss/index.faiss",
            documents_path: str = "data/faiss/documents.json",
            metadata_path: str = "data/faiss/metadata.json"
    ):
        self.dimension = dimension
        self.index_path = Path(index_path)
        self.documents_path = Path(documents_path)
        self.metadata_path = Path(metadata_path)

        self.index_path.parent.mkdir(
            parents=True,
            exist_ok=True
        )

        self.indexed_files = set()

        if (
                self.index_path.exists()
                and self.documents_path.exists()
        ):
            print("Loading existing FAISS vector store...")
            self.index = faiss.read_index(
                str(self.index_path)
            )
            self.documents = json.loads(
                self.documents_path.read_text(
                    encoding="utf-8"
                )
            )
            if self.metadata_path.exists():
                try:
                    meta = json.loads(
                        self.metadata_path.read_text(encoding="utf-8")
                    )
                    self.indexed_files = set(meta.get("indexed_files", []))
                except Exception as e:
                    print(f"Error loading metadata: {e}")
        else:
            print("Creating new FAISS vector store...")
            self.index = faiss.IndexFlatL2(dimension)
            self.documents = []

    def is_file_indexed(self, doc_identifier: str) -> bool:
        return doc_identifier in self.indexed_files

    def mark_file_indexed(self, doc_identifier: str):
        self.indexed_files.add(doc_identifier)

    def add_vectors(self, vectors, documents, doc_identifier: str = None):
        if not vectors or not documents:
            return

        existing_doc_set = set(self.documents)
        new_vectors = []
        new_docs = []

        for vec, doc in zip(vectors, documents):
            if doc not in existing_doc_set:
                new_vectors.append(vec)
                new_docs.append(doc)
                existing_doc_set.add(doc)

        if not new_docs:
            print("No new unique chunks to add.")
            if doc_identifier:
                self.mark_file_indexed(doc_identifier)
                self.save_metadata()
            return

        vectors_array = np.array(
            new_vectors,
            dtype="float32"
        )

        self.index.add(vectors_array)
        self.documents.extend(new_docs)

        if doc_identifier:
            self.mark_file_indexed(doc_identifier)

        self.save_store()

    def save_store(self):
        faiss.write_index(
            self.index,
            str(self.index_path)
        )
        self.documents_path.write_text(
            json.dumps(
                self.documents,
                ensure_ascii=False,
                indent=2
            ),
            encoding="utf-8"
        )
        self.save_metadata()
        print("FAISS vector store saved successfully.")

    def save_metadata(self):
        self.metadata_path.write_text(
            json.dumps(
                {"indexed_files": list(self.indexed_files)},
                ensure_ascii=False,
                indent=2
            ),
            encoding="utf-8"
        )

    def search(self, query_vector, top_k: int = 3):
        if self.index.ntotal == 0:
            return []

        fetch_k = min(self.index.ntotal, max(top_k * 5, 20))
        query_array = np.array(
            [query_vector],
            dtype="float32"
        )

        distances, indices = self.index.search(
            query_array,
            fetch_k
        )

        results = []
        seen_texts = set()

        for distance, index in zip(
                distances[0],
                indices[0]
        ):
            if index == -1 or index >= len(self.documents):
                continue

            doc_text = self.documents[index]
            norm_text = doc_text.strip().lower()

            if norm_text in seen_texts:
                continue

            seen_texts.add(norm_text)
            results.append({
                "document": doc_text,
                "distance": float(distance)
            })

            if len(results) >= top_k:
                break

        return results