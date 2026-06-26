#
# Copyright (c) 2023-2026 CLOUDRISK Limited and FT Advisory LLC
# SPDX-License-Identifier: Apache-2.0
#

"""Demonstrates a runtime polymorphism issue independent of the `as` operator.

`BaseDataClass.model_config` (rune-python-runtime) sets `revalidate_instances=
'always'` together with `extra='ignore'`. Combined with `@validate_call` on
every generated function, this means: passing a subtype instance (`Bar`) into
a parameter declared as its supertype (`Foo`) causes Pydantic to rebuild the
value strictly from `Foo`'s own schema, discarding the subtype's identity and
its extra fields — before the function body ever runs.

This violates the Liskov Substitution Principle's expectation that a subtype
passed where a supertype is expected remains substitutable *as that subtype*.
It does not require the `as` operator to reproduce: a plain identity function
and a single `isinstance` check are enough.

See the discussion in docs/AS_OPERATOR_IMPACT.md for how this surfaced via
`as` data type narrowing, and why choice narrowing is unaffected (it never
crosses a supertype/subtype parameter boundary).
"""

import pytest

from rosetta_dsl.test.expressions.polymorphism_demo.Bar import Bar
from rosetta_dsl.test.expressions.polymorphism_demo.functions.IdentityFoo import (
    IdentityFoo,
)


"""
@pytest.mark.xfail(
    reason=(
        "BaseDataClass.model_config sets revalidate_instances='always' + "
        "extra='ignore', so @validate_call rebuilds any subtype instance "
        "passed through a supertype-typed parameter as a plain instance of "
        "the declared supertype, discarding its actual runtime type and "
        "fields. Fixing this requires a rune-python-runtime change."
    ),
    strict=True,
)
"""
def test_subtype_identity_lost_across_function_boundary():
    """A Bar passed into a Foo-typed parameter should still be a Bar on return."""
    bar = Bar(barAttr=42)
    assert isinstance(bar, Bar)  # sanity check: true before the call

    result = IdentityFoo(foo=bar)

    # Expected under standard OOP/Liskov semantics: the identity function
    # returns the same kind of object it was given.
    assert isinstance(result, Bar)


if __name__ == "__main__":
    test_subtype_identity_lost_across_function_boundary()
