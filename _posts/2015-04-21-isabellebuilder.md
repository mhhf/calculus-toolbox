---
layout: page
title: "IsabelleBuilder"
category: ref
date: 2015-04-21 11:19:29
---

This class ([`tools/isabuilder.py`](https://github.com/goodlyrottenapple/calculus-toolbox/blob/master/tools/isabuilder.py)) contains all the macro functions, which take the JSON encoded calculus and generate Isabelle code that is replaced into the `.thy` theory files.

Add any macro functions for code generation in Isabelle theories to this [file (`tools/isabuilder.py`)](https://github.com/goodlyrottenapple/calculus-toolbox/blob/master/tools/isabuilder.py). They will automatically be called if referenced in the processed file (i.e if a method `foo` is defined, it will be called if `(*foo*)` appears in the processed file).  
For passing arguments to `foo`, use `(*foo?arg1?arg2?...*)` in the processed file.  
The return type for any function that can be referenced in the processed files must be string. For any functions that do not return a string, add two underscores before a function name. Also add two underscores if the function is going to be static or purposefully hidden from use in the processed file.

<br>

{: .code .python #init}
| def \_\_init\_\_(self, file) |

Parses the file (under `file` path) as a JSON file and stores the contents in `self.calc`  
__Also normalizes the values stored under the `type` key, which can either be strings or list of strings__

<br>

{: .code .python #add}
| def add(self, key, val) |

Adds a (key, value) pair to the calculus definition dictionary (i.e. set 'export_path')  
__Use strings as keys, otherwise the pair won't be added.__

<br>

{: .code .python #get}
| def get(self, key) |

Returns the value for key stored in `self.calc` dictionary

<br>

{: .code .python #calc_name_core}
| def calc_name_core(self) |

Returns `calc_name` from `self.calc` in the form `{calc_name}_Core`  
__Requires `calc_name` to be defined__

<br>

{: .code .python #calc_name}
| def calc_name(self) |

Returns `calc_name` from `self.calc` in the form `{calc_name}`  
__Requires `calc_name` to be defined__


<br>

{: .code .python #export_path}
| def export_path(self) |

Returns a string of the form `"{export_path}/{calc_name}.scala"` (used in Isabelle `export_code` function)  
__Requires `calc_name` and `export_path` to be defined__

<br>

<div class="code">@staticmethod</div>

{: .code .python #keywords}
| def __keywords(calc) |

Given a dictionary, this function returns all the keys of the dictionary and recursively returns keys of any sub-dictionaries

<br>

{: .code .python #uncomment}
| def uncommentL(self, list) |

{: .code .python #uncomment}
| def uncommentR(self, list) |

These functions in pair uncomment a section enclosed in:

~~~
(*(*uncommentL?ident1?ident2?ident3?...*) ... (*uncommentR?ident1?ident2?ident3?...*)*)
~~~

if `ident1,ident2,ident3,...` is defined in `calc_structure` (that is $\\{ ident1,ident2,ident3,... \\} \subseteq set( \text{__keywords(self.calc)})$), by turning into:

~~~
(*(*uncommentL?ident1?ident2?ident3?...-BEGIN*)*)(*uncommentL?ident1?ident2?ident3?...-END*) ... (*uncommentR?ident1?ident2?ident3?...-BEGIN*)(*(*uncommentR?ident1?ident2?ident3?...-END*)*)
~~~

Otherwise they remain unchanged.

<br>

<div class="code">@staticmethod</div>

{: .code .python #calc_structure_dependencies}
| def __calc_structure_dependencies(structure) |

Given a dictionary (`structure`) of a calculus structure, returns a new dictionary of all the data types declared in the `structure` dictionary, along with their dependencies on any other data types.  
Used in [calc_structure](#calc_structure) and [calc_structure_rules](#calc_structure_rules)

<br>

<div class="code">@staticmethod</div>

{: .code .python #calc_structure_datatype}
| def __calc_structure_datatype(name, datatype) |

Returns a definition of a `datatype` in Isabelle with syntactic sugar (if defined).
For example, the following entry in the JSON file:

~~~json
"Formula_Bin_Op" : {
    "Formula_And" : {
        "isabelle" : "\\<and>\\<^sub>F",
        "ascii" : "^",
        "latex" : "\\wedge"
    },
    "Formula_ImpR" : {
        "isabelle" : "\\<rightarrow>\\<^sub>F",
        "ascii" : ">",
        "latex" : "\\rightarrow"
    }
}
~~~

would produce this output:

~~~json
datatype Formula_Bin_Op = Formula_And ("\<and>\<^sub>F")
                        | Formula_ImpR ("\<rightarrow>\<^sub>F")
~~~


<br>

{: .code .python #calc_structure}
| def calc_structure(self) |

Called from the [core calculus template](https://github.com/goodlyrottenapple/calculus-toolbox/blob/master/template/Calc_Core.thy). Generates Isabelle `datatype` definitions declared under `calc_structure` in the JSON calculus file.  
Uses [__calc_structure_datatype](#calc_structure_datatype) and [__calc_structure_dependencies](#calc_structure_dependencies)

<br>

<div class="code">@staticmethod</div>

{: .code .python #calc_structure_all_rules}
| def __calc_structure_all_rules(rules) |

Given a dictionary of [rule categories/groups]({{ site.baseurl }}/doc/calculus-encoding.html#calcstructurerules) (defined in `calc_structure_rules`) generates the `datatype Rules` and `Prooftree`.

Given the rule groups defined in the [default JSON file](https://github.com/goodlyrottenapple/calculus-toolbox/blob/master/default.json), this function will produce:

~~~isabelle
datatype Rule = RuleZer RuleZer
              | RuleCut RuleCut
              | RuleU RuleU
              | RuleDisp RuleDisp
              | RuleOp RuleOp
              | RuleBin RuleBin
              | RuleMacro string Prooftree
              | Fail
     and Prooftree = Prooftree Sequent Rule "Prooftree list" ("_ \<Longleftarrow> PT ( _ ) _" [341,341] 350)
~~~

<br>

{: .code .python #calc_structure_rules_concl}
| def __calc_structure_rules_concl(self)

__Deprecated__

<br>

{: .code .python #calc_structure_rules}
| def calc_structure_rules(self) |

Called from the [calculus rules template](https://github.com/goodlyrottenapple/calculus-toolbox/blob/master/template/Calc_Rules.thy). Generates Isabelle `datatype` definitions declared under `calc_structure_rules` in the JSON calculus file.    
Uses [__calc_structure_datatype](#calc_structure_datatype), [__calc_structure_dependencies](#calc_structure_dependencies) and [__calc_structure_all_rules](#calc_structure_all_rules)

<br>

<div class="code">@staticmethod</div>

{: .code .python #calc_structure_rules_rule_list_aux}
| def __calc_structure_rules_rule_list_aux(name, rules, rule_def, parser_command) |

This function takes in the `parser_command` string, which it uses to parse the rules encoded in the `rules` dictionary and generate function of the following form:

~~~json
primrec rule{name} :: "Locale \<Rightarrow> {name} \<Rightarrow> ruleder" where
"rule{name} x {name}.{rule} = ..."
~~~

Since the rule encoding in the JSON file is split into two parts, the declaration of the rule in `calc_structure_rules` and the ASCII encoding in `rules`, these two corresponding dictionaries are passed in as `rules` and `rule_def`.

<br>

{: .code .python #rules_rule_fun}
| def rules_rule_fun(self) |

<br>

{: .code .python #rules_rule_list}
| def rules_rule_list(self) |

<br>