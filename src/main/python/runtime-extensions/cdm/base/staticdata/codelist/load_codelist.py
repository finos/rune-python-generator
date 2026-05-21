import json
import logging
import re
from functools import lru_cache
from importlib import resources as resource_loader
from typing import cast

# Codelist object for deserialization 
from finos.cdm.base.staticdata.codelist.CodeList import CodeList

# Configure logging
logging.basicConfig (
    level=logging.WARNING,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

# Dynamically locate the 'finos' CDM package in the environment. Then navigate to the nested resources folder
codelist_dir = resource_loader.files("finos").joinpath("resources", "codelist", "json")

# Add the LRU Cache. 
# maxsize=15 means it will remember the last 128 requested domains in memory, evicting the least recently used (lcu) ones when the limit is exceeded.
@lru_cache(maxsize=15)
def load_codelist(domain: str) -> CodeList:
    """
    Loads a JSON CodeList based on domain and deserializes it into a CDM CodeList.
    Results are cached to prevent redundant disk I/O and deserialization overhead.
    """
    # If you see this log, it means the cache was empty for this domain (Cache Miss).
    # If you call the function again with the same domain, it skips the whole function 
    # and returns the memory object instantly (Cache Hit), printing nothing.
    logger.debug(f"Cache miss: Loading CodeList for domain '{domain}' from disk.")
    
    target_file = None

    # Dynamically builds a pattern looking exactly for "domain-X-Y.json"
    pattern = re.compile(rf"^{re.escape(domain.lower())}-\d+-\d+\.json$")
    
    # Search for the JSON file inside the wheel
    for file_path in codelist_dir.iterdir():
        if file_path.name.endswith(".json") and pattern.match(file_path.name):
            target_file = file_path
            break
            
    # If no file matching, log an error and raise an exception
    if not target_file:
        logger.error(f"Domain '{domain}' not found in {codelist_dir}")
        raise FileNotFoundError(f"Could not find CodeList JSON for domain: '{domain}'")
        
    # Open and load the JSON directly from the package
    try:
        with target_file.open('r', encoding='utf-8') as f:
            json_raw_data = json.load(f)
    except json.JSONDecodeError as e:
        logger.error(f"Failed to parse JSON in {target_file.name}: {e}")
        raise 
        
    # Use the inherited Rune deserialization method
    try:
        cdm_object = CodeList.rune_deserialize(json_raw_data)
        logger.debug(f"Successfully deserialized CodeList for '{domain}'.")
        return cast(CodeList, cdm_object)
    except Exception as e:
        logger.error(f"Rune deserialization failed for '{domain}': {e}")
        raise