class test_generated_syntax_basic_BasicList(BaseDataClass):
    booleanTypes: list[bool] = Field([], description='', min_length=1)
    numberTypes: list[Decimal] = Field([], description='', min_length=1)
    parameterisedNumberTypes: list[Annotated[Decimal, Field(max_digits=18, decimal_places=2)]] = Field([], description='', min_length=1)
    parameterisedStringTypes: list[Annotated[str, Field(min_length=1, pattern=r'^[a-zA-Z]*$', max_length=20)]] = Field([], description='', min_length=1)
    stringTypes: list[str] = Field([], description='', min_length=1)
    timeTypes: list[datetime.time] = Field([], description='', min_length=1)