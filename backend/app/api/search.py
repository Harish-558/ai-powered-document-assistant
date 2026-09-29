from fastapi import APIRouter, HTTPException

from app.models.search import SearchQuery
from app.services.vector_search import search_vectors

router = APIRouter(
    prefix="/search",
    tags=["Search"]
)


@router.post("/")
async def search_documents(query: SearchQuery):

    if not query.query.strip():
        raise HTTPException(
            status_code=400,
            detail="Search query cannot be empty"
        )

    results = search_vectors(
        query.query,
        top_k=query.top_k
    )

    return {
        "query": query.query,
        "results": results
    }