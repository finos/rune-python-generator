import logging

# Import custom implementations
from finos.runtime_extensions.cdm.base.staticdata.codelist.load_codelist import load_codelist as impl_load_codelist

# Import the generated CDM facades (proxies)
import finos.cdm.base.staticdata.codelist.functions.LoadCodeList as cdm_load_codelist_facade 

logger = logging.getLogger(__name__)

def setup_cdm_runtime():
    """
    Python equivalent of CdmRuntimeModule.java.
    Wires custom native implementations into the auto-generated CDM Facades.
    """
    logger.info("Initializing CDM Python Runtime...")

    # Inject LoadCodeList
    if hasattr(cdm_load_codelist_facade, "LoadCodeList"):
        cdm_load_codelist_facade.LoadCodeList.__assign__(impl_load_codelist) # type: ignore
        logger.debug("Successfully bound LoadCodeList.")
    else:
        logger.error("Bootstrap Failed: Could not find LoadCodeList in CDM Facade.")

    # Future injections (like RoundToNearest) will go here

    logger.info("Succesfully initialized CDM Python Runtime!!!")