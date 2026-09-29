import hashlib
from pathlib import Path

from app.services.embedding_service import generate_embedding
from app.services.text_chunker import split_text_into_chunks
from app.services.document_processor import extract_text_from_pdf
from app.vectorstore.faiss_store import FAISSVectorStore


class DocumentVectorSearch:

    def __init__(self):
        self.store = FAISSVectorStore()

    def add_document(self, file_path: str):
        path = Path(file_path)
        file_bytes = path.read_bytes()
        file_hash = hashlib.md5(file_bytes).hexdigest()
        doc_identifier = f"{path.name}_{file_hash}"

        # 1. Extract text from PDF
        text = extract_text_from_pdf(file_path)

        # 2. Split text into chunks (~300-500 chars)
        chunks = split_text_into_chunks(
            text,
            chunk_size=400,
            overlap=50
        )

        if not chunks:
            return 0

        # Check if already indexed
        if self.store.is_file_indexed(doc_identifier):
            print(f"Document '{path.name}' already indexed. Skipping re-indexing.")
            return len(chunks)

        # Filter out chunks that exist in vector store
        existing_set = set(self.store.documents)
        new_chunks = [c for c in chunks if c not in existing_set]

        if new_chunks:
            vectors = [
                generate_embedding(chunk)
                for chunk in new_chunks
            ]

            self.store.add_vectors(
                vectors,
                new_chunks,
                doc_identifier=doc_identifier
            )
        else:
            self.store.mark_file_indexed(doc_identifier)
            self.store.save_metadata()

        return len(chunks)

    def search(self, query: str, top_k: int = 3):
        query_vector = generate_embedding(query)
        return self.store.search(
            query_vector,
            top_k
        )


document_search = DocumentVectorSearch()


def search_vectors(query: str, top_k: int = 3):
    return document_search.search(query, top_k)