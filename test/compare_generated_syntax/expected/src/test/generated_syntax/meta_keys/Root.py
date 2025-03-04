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
__all__ = ['Root']


class Root(BaseDataClass):
    nodeRef: Optional[Annotated[test.generated_syntax.meta_keys.NodeRef.NodeRef, test.generated_syntax.meta_keys.NodeRef.NodeRef.serializer(), test.generated_syntax.meta_keys.NodeRef.NodeRef.validator()]] = Field(None, description='')
    attributeRef: Optional[Annotated[test.generated_syntax.meta_keys.AttributeRef.AttributeRef, test.generated_syntax.meta_keys.AttributeRef.AttributeRef.serializer(), test.generated_syntax.meta_keys.AttributeRef.AttributeRef.validator()]] = Field(None, description='')
