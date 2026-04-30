"""Language code mappings for NLLB-200 and Whisper.

NLLB-200 uses FLORES-200 codes (e.g. ``eng_Latn``, ``zho_Hans``).
Whisper uses ISO 639-1 codes (e.g. ``en``, ``zh``) or full language names.

Supported languages for v1:
    English, Chinese (Simplified), Vietnamese, Sinhala
"""

from dataclasses import dataclass
from typing import Optional


@dataclass(frozen=True)
class Language:
    """Represents a supported language with codes for both ASR and NMT."""

    code: str          # ISO 639-1 / Whisper code (en, zh, vi, si)
    name: str          # English display name
    native_name: str   # Native-script display name
    nllb_code: str     # NLLB-200 FLORES-200 code


SUPPORTED_LANGUAGES: list[Language] = [
    Language(code="en",  name="English",              native_name="English",    nllb_code="eng_Latn"),
    Language(code="zh",  name="Chinese (Simplified)",  native_name="普通话",      nllb_code="zho_Hans"),
    Language(code="vi",  name="Vietnamese",            native_name="Tiếng Việt", nllb_code="vie_Latn"),
    Language(code="si",  name="Sinhala",               native_name="සිංහල",       nllb_code="sin_Sinh"),
]


def get_language(whisper_code: str) -> Optional[Language]:
    """Look up a Language by its Whisper/ISO code."""
    for lang in SUPPORTED_LANGUAGES:
        if lang.code == whisper_code:
            return lang
    return None


def whisper_to_nllb(whisper_code: str) -> str:
    """Convert a Whisper language code to an NLLB-200 FLORES-200 code.

    Returns the input unchanged if it's already an NLLB code.
    """
    lang = get_language(whisper_code)
    if lang:
        return lang.nllb_code
    return whisper_code


def nllb_to_whisper(nllb_code: str) -> str:
    """Convert an NLLB-200 FLORES-200 code to a Whisper/ISO code."""
    for lang in SUPPORTED_LANGUAGES:
        if lang.nllb_code == nllb_code:
            return lang.code
    return nllb_code
