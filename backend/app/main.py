from fastapi import FastAPI

from app.api.documents import router as documents_router
from app.api.search import router as search_router


app = FastAPI(
    title="Document Assistant API",
    description="Backend API for document processing and vector search",
    version="1.0.0"
)


app.include_router(documents_router)
app.include_router(search_router)


@app.get("/")
def root():
    return {
        "message": "Document Assistant API is running"
    }


@app.get("/health")
def health_check():
    return {
        "status": "healthy"
    }