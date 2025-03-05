class Choice(BaseDataClass):
    intType: Optional[int] = Field(None, description='')
    stringType: Optional[str] = Field(None, description='')
    
    @rune_condition
    def condition_0_Choice(self):
        item = self
        return rune_check_one_of(self, 'intType', 'stringType', necessity=True)