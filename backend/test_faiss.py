from app.services.embedding_service import generate_embedding
from app.vectorstore.faiss_store import FAISSVectorStore


store = FAISSVectorStore()


documents = [
    "Kotlin is used for Android application development.",
    "Python is commonly used for backend development.",
    "FAISS is used for vector similarity search."
]


vectors = [
    generate_embedding(document)
    for document in documents
]


store.add_vectors(
    vectors,
    documents
)


query = "Android development using Kotlin"

query_vector = generate_embedding(query)


results = store.search(
    query_vector,
    top_k=2
)


print("\nSearch Results:")

for result in results:
    print(result)