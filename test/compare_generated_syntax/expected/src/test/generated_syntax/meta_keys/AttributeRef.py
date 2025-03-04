# pylint: disable=line-too-long, invalid-name, missing-function-docstring
# pylint: disable=bad-indentation, trailing-whitespace, superfluous-parens
# pylint: disable=wrong-import-position, unused-import, unused-wildcard-import
# pylint: disable=wildcard-import, wrong-import-order, missing-class-docstring
# pylint: disable=missing-module-docstring
from __future__ import annotations
from typing import Optional, Annotated
import datetime
import inspect
from decimal import Decimal
from pydantic import Field
from rune.runtime.base_data_class import BaseDataClass
from rune.runtime.metadata import *
from rune.runtime.utils import *
from rune.runtime.conditions import *
from rune.runtime.func_proxy import *
__all__ = ['AttributeRef']


class AttributeRef(BaseDataClass):
    dateField: Optional[Annotated[DateWithMeta, DateWithMeta.serializer(), DateWithMeta.validator(('@key', '@key:external'))]] = Field(None, description='')
    dateReference: Optional[Annotated[DateWithMeta, DateWithMeta.serializer(), DateWithMeta.validator(('@ref', '@ref:external'))]] = Field(None, description='')
    
    _KEY_REF_CONSTRAINTS = {
      'dateField': {'@key', '@key:external'},
      'dateReference': {'@ref', '@ref:external'}
    }

import test 