from fastapi import APIRouter, UploadFile, File, HTTPException
from pathlib import Path
import shutil

from app.services.document_processor import extract_text_from_pdf
from app.services.text_chunker import split_text_into_chunks
from app.services.vector_search import document_search

router = APIRouter(
    prefix="/documents",
    tags=["Documents"]
)


DOCUMENTS_DIR = Path("data/documents")
DOCUMENTS_DIR.mkdir(parents=True, exist_ok=True)


@router.post("/upload")
async def upload_document(file: UploadFile = File(...)):

    if not file.filename:
        raise HTTPException(
            status_code=400,
            detail="No file selected"
        )

    file_path = DOCUMENTS_DIR / file.filename

    with file_path.open("wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    chunk_count = document_search.add_document(str(file_path))
    return {
        "message": "Document uploaded and indexed successfully",
        "filename": file.filename,
        "path": str(file_path),
        "chunks_indexed": chunk_count
    }
@router.get("/{filename}/text")
async def get_document_text(filename: str):



    file_path = DOCUMENTS_DIR / filename

    if not file_path.exists():
        raise HTTPException(
            status_code=404,
            detail="Document not found"
        )

    if file_path.suffix.lower() != ".pdf":
        raise HTTPException(
            status_code=400,
            detail="Text extraction currently supports PDF files only"
        )

    text = extract_text_from_pdf(str(file_path))

    return {
        "filename": filename,
        "text": text
    }
@router.get("/{filename}/chunks")
async def get_document_chunks(filename: str):

    file_path = DOCUMENTS_DIR / filename

    if not file_path.exists():
        raise HTTPException(
            status_code=404,
            detail="Document not found"
        )

    if file_path.suffix.lower() != ".pdf":
        raise HTTPException(
            status_code=400,
            detail="Chunking currently supports PDF files only"
        )

    text = extract_text_from_pdf(str(file_path))

    chunks = split_text_into_chunks(
        text,
        chunk_size=400,
        overlap=50
    )

    return {
        "filename": filename,
        "chunk_count": len(chunks),
        "chunks": chunks
    }