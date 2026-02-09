from __future__ import annotations
from typing import Annotated
class B:
    print('Defining B')
    x: Annotated['A', A.foo()]
class A:
    @staticmethod
    def foo(): return 1
print('SUCCESS')
