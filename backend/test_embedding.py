from app.services.embedding_service import generate_embedding


text = "This is a document about Android development using Kotlin."

embedding = generate_embedding(text)

print("Embedding generated successfully!")
print("Vector size:", len(embedding))
print("First 5 values:", embedding[:5])