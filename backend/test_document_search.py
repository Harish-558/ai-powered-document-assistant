from app.services.vector_search import DocumentVectorSearch


PDF_PATH = "data/documents/Yalagala_Naga_Bhushanam_Resume(1).pdf"


search_engine = DocumentVectorSearch()


print("Adding PDF to vector store...")

chunk_count = search_engine.add_document(PDF_PATH)

print(f"PDF added successfully!")
print(f"Total chunks: {chunk_count}")


query = "What are the person's skills and technical experience?"

print("\nSearching for:")
print(query)

results = search_engine.search(
    query,
    top_k=3
)


print("\nSearch Results:")

for i, result in enumerate(results, start=1):

    print(f"\nResult {i}")
    print("Distance:", result["distance"])
    print("Text:")
    print(result["document"])