import re


def split_text_into_chunks(
    text: str,
    chunk_size: int = 400,
    overlap: int = 50
) -> list[str]:
    """
    Split text into overlapping character chunks of approx 300-500 characters,
    respecting sentence and word boundaries.
    """
    if not text or not text.strip():
        return []

    if overlap >= chunk_size:
        raise ValueError("overlap must be smaller than chunk_size")

    # Clean text: normalize whitespace and newlines slightly
    clean_text = re.sub(r'[ \t]+', ' ', text).strip()
    clean_text = re.sub(r'\n{3,}', '\n\n', clean_text)

    if len(clean_text) <= chunk_size:
        return [clean_text]

    chunks = []
    start = 0
    text_len = len(clean_text)

    while start < text_len:
        end = start + chunk_size

        if end >= text_len:
            chunk = clean_text[start:].strip()
            if chunk:
                chunks.append(chunk)
            break

        # Look for a natural break point (paragraph break, sentence end, or word boundary)
        min_break = start + (chunk_size // 3)
        break_point = -1

        for delims in ['\n\n', '\n', '. ', '? ', '! ']:
            pos = clean_text.rfind(delims, min_break, end)
            if pos != -1:
                break_point = pos + len(delims)
                break

        if break_point == -1:
            pos = clean_text.rfind(' ', min_break, end)
            if pos != -1:
                break_point = pos + 1
            else:
                break_point = end

        chunk = clean_text[start:break_point].strip()
        if chunk:
            chunks.append(chunk)

        new_start = break_point - overlap
        if new_start <= start or new_start >= text_len:
            new_start = break_point

        start = new_start

    deduped_chunks = []
    for c in chunks:
        if not deduped_chunks or deduped_chunks[-1] != c:
            deduped_chunks.append(c)

    return deduped_chunks