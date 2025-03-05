# _Serialization Update_

_What is being released?_

This release adds support for meta data and for serialization / deserialization consistient with the [serialization specifications in CDM issue #3236](https://github.com/finos/common-domain-model/issues/3236)

# Reading From and Writing To a String

The generated Python code can deserialize and serialize an object.

## Deserializing from a string

To deserialize from a string and create a object of the model specified in the string invoke the function:

`BaseDataClass.rune_deserialize` with the following parameters

    rune_json (str): A JSON string.

    validate_model (bool, optional): Validate that the model passses all Rune defined constraints after deserializing.  Setting to False allows for creation of an invalid Model.  Defaults to True.

    strict (bool, optional): Ensure that all input types strictly match the Rune definition.  If set to False, deserialization will attempt to convert types (i.e. convert a string to an int).  Defaults to True.

    raise_validation_exceptions (bool, optional): Raise an exception should there be validation error. Setting to False generates a list of errors and allows for creation of an invalid model.  Defaults to True.

    Returns:
      BaseModel: The Rune model.

To serialize from an object ("[obj]") of a generated class, invoke the function:

`[obj].rune_serialize` with the following parameters:

    validate_model (bool, optional): Validate that the model passes all Rune defined constraings prior to serialization. Setting to False allows serialization of an invalid Model. Defaults to True.

    strict (bool, optional): Ensure that all input types strictly match the Rune definition.  Setting to False saves fields with types that do not match the Model definition.  Defaults to True.

    raise_validation_exceptions (bool, optional): Raise an exception should there be validation error. Setting to False generates a list of errors and allows for serialzation of an invalid model.  Defaults to True.

    indent (int | None, optional): Indentation to use in the JSON output. If None is passed, the output will be compact. Defaults to None.

    serialize_as_any (bool, optional): Instructs serialization to attempt to convert types to align to the Model defintion.  Defaults to False.

    include (IncEx | None, optional): Field(s) to include in the JSON output.  Ignored if not specified.  Defaults to None.

    exclude (IncEx | None, optional): Field(s) to exclude from the JSON output. Ignored if not specified.  Defaults to None.

    exclude_unset (bool, optional): Exclude fields that have not been explicitly set. Defaults to True.

    round_trip (bool, optional): If True, checks that the serialized output can be used to create a valid object.  Defaults to False.

    warnings (bool | Literal['none', 'warn', 'error'], optional): Determines how to handle serialization errors. There are three options:
    - False/"none" silently allows the serialization of an invalid Model.
    - True/"warn" logs errors and allows the serialization of an invalid Model.
    - "error" fails to save an invalid Model and raises a PydanticSerializationError`
    Defaults to True.

    exclude_defaults (bool, optional): Determines whether to exclude fields that are set to their default value. If False, fields that have default values because they not been explictly set will be included.  Defaults to True.

    exclude_none (bool, optional): Determines whether to exclude fields that have a value of `None`. If True, fields set to None will be included.  Defaults to False.


    Returns:
      A string.

