class test_generated_syntax_basic_BasicSingle(BaseDataClass):
    booleanType: bool = Field(..., description='')
    numberType: Decimal = Field(..., description='')
    parameterisedNumberType: Decimal = Field(..., description='', max_digits=18, decimal_places=2)
    parameterisedStringType: str = Field(..., description='', min_length=1, pattern=r'^[a-zA-Z]*$', max_length=20)
    stringType: str = Field(..., description='')
    timeType: datetime.time = Field(..., description='')