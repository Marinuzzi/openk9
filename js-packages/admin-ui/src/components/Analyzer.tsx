import React from "react";
import { gql } from "@apollo/client";
import { Link, useNavigate, useParams } from "react-router-dom";
import form from "@clayui/form";
import {
  useAnalyzerQuery,
  useBindTokenizerToAnalyzerMutation,
  useCreateOrUpdateAnalyzerMutation,
  useTokenizerOptionsQuery,
  useTokenizerValueQuery,
  useUnbindnTokenizerToAnalyzerMutation,
} from "../graphql-generated";
import {
  useForm,
  fromFieldValidators,
  TextInput,
  TextArea,
  SearchSelect,
  KeyValue,
  MultiSelectForDinamicallyFieldsWithoutType,
  CreateDinamicallyFieldWithout,
  MainTitle,
  CustomButtom,
  ContainerFluid,
} from "./Form";
import { useToast } from "./ToastProvider";
import { AnalyzersQuery } from "./Analyzers";
import { ClassNameButton } from "../App";
import { queryAnalysisConfigOptions } from "./Bucket";

const AnalyzerQuery = gql`
  query Analyzer($id: ID!) {
    analyzer(id: $id) {
      id
      name
      type
      description
      jsonConfig
      tokenizer {
        id
      }
    }
  }
`;

gql`
  mutation CreateOrUpdateAnalyzer($id: ID, $name: String!, $description: String, $type: String!, $jsonConfig: String) {
    analyzer(id: $id, analyzerDTO: { name: $name, description: $description, type: $type, jsonConfig: $jsonConfig }) {
      entity {
        id
        name
      }
      fieldValidators {
        field
        message
      }
    }
  }
`;

export function Analyzer() {
  const { analyzerId = "new" } = useParams();
  const navigate = useNavigate();
  const showToast = useToast();
  const analyzerQuery = useAnalyzerQuery({
    variables: { id: analyzerId as string },
    skip: !analyzerId || analyzerId === "new",
  });
  const [templateChoice, setTemplateChoice] = React.useState<KeyValue>(
    JSON.parse(analyzerQuery.data?.analyzer?.jsonConfig || `${TemplateAnalyzers[0].Json}`)
  );
  const [IsEmpty, setIsEmpty] = React.useState(false);
  const [isCustom, setIsCustom] = React.useState(false);
  const [createOrUpdateAnalyzerMutate, createOrUpdateanalyzerMutation] = useCreateOrUpdateAnalyzerMutation({
    refetchQueries: [AnalyzerQuery, AnalyzersQuery],
    onCompleted(data) {
      if (data.analyzer?.entity) {
        if (analyzerId === "new") {
          navigate(`/analyzers/`, { replace: true });
          showToast({ displayType: "success", title: "Analyzer created", content: data.analyzer.entity.name ?? "" });
        } else {
          showToast({ displayType: "info", title: "Analyzer updated", content: data.analyzer.entity.name ?? "" });
        }
      }
    },
  });
  const form = useForm({
    initialValues: React.useMemo(
      () => ({
        name: "",
        description: "",
        type: "",
        jsonConfig: "{}",
      }),
      []
    ),
    originalValues: analyzerQuery.data?.analyzer,
    isLoading: analyzerQuery.loading || createOrUpdateanalyzerMutation.loading,
    onSubmit(data) {
      createOrUpdateAnalyzerMutate({
        variables: { id: analyzerId !== "new" ? analyzerId : undefined, ...data },
      });
    },
    getValidationMessages: fromFieldValidators(createOrUpdateanalyzerMutation.data?.analyzer?.fieldValidators),
  });
  const [type, setType] = React.useState("{}");

  React.useEffect(() => {
    if (JSON.stringify(templateChoice) !== "{}") {
      form.inputProps("jsonConfig").onChange(JSON.stringify(templateChoice));
    }
  }, [templateChoice]);
  if (analyzerQuery.loading) {
    return <div></div>;
  }
  if (!analyzerQuery.loading && analyzerId !== "new" && type === "{}") {
    setType(form.inputProps("type").value);
    if (form.inputProps("type").value === "custom") {
      setIsCustom(true);
    }
  }
  if (analyzerId !== "new" && JSON.stringify(templateChoice) === "{}" && IsEmpty === false) {
    try {
      const value = JSON.parse(form.inputProps("jsonConfig").value);
      setTemplateChoice(value);
      setIsEmpty(true);
    } catch (error) {}
  }

  if (analyzerId !== "new") {
    TemplateAnalyzers.forEach((filter) => {
      if (filter.title === type) {
        filter.visible = "" + true;
      }
    });
  }

  return (
    <>
      {analyzerId !== "new" && (
        <div
          className="navbar navbar-underline navigation-bar navigation-bar-secondary navbar-expand-md"
          style={{
            position: "sticky",
            backgroundColor: "white",
            boxShadow: "rgb(194 177 177 / 61%) 3px 4px 3px",
            top: "59px",
            zIndex: "1",
            borderTop: "1px solid #00000024",
          }}
        >
          <style type="text/css">
            {`
                .navbar-underline.navbar-expand-md .navbar-nav .nav-link.active:after{
                  background-color: red;
                }
                .navigation-bar-secondary .navbar-nav .nav-link.active {
                  color: black;
                }
                .navigation-bar-secondary .navbar-nav .nav-link:hover {
                  color: black;
                }
          `}
          </style>
          <div className="container-fluid container-fluid-max-xl">
            <ul className="navbar-nav ">
              <li className="nav-item ">
                <Link className={isCustom ? "nav-link  active" : "nav-link disabled"} to={``}>
                  <span className="navbar-text-truncate active">{"Attributes"}</span>
                </Link>
              </li>
              <li className="nav-item">
                <Link className={isCustom ? "nav-link  " : "nav-link disabled"} to={`/analyzers/${analyzerId}/char-filters`}>
                  <span className="navbar-text-truncate ">{"Char Filters"}</span>
                </Link>
              </li>
              <li className="nav-item">
                <Link className={isCustom ? "nav-link  " : "nav-link disabled"} to={`/analyzers/${analyzerId}/token-filters`}>
                  <span className="navbar-text-truncate ">{"Token Filters"}</span>
                </Link>
              </li>
            </ul>
          </div>
        </div>
      )}
      <ContainerFluid>
        {analyzerId !== "new" && <MainTitle title="Attributes" />}
        <form
          className="sheet"
          onSubmit={(event) => {
            event.preventDefault();
            form.submit();
          }}
        >
          <TextInput label="Name" {...form.inputProps("name")} />
          <TextArea label="Description" {...form.inputProps("description")} />
          <MultiSelectForDinamicallyFieldsWithoutType
            id={analyzerId}
            setTitle={form.inputProps("name").onChange}
            type={type}
            setIsCustom={setIsCustom}
            template={TemplateAnalyzers}
            onChangeDescription={form.inputProps("description").onChange}
            setTemplateChoice={setTemplateChoice}
            onChangeType={form.inputProps("type").onChange}
          />
          <CreateDinamicallyFieldWithout
            templates={TemplateAnalyzers}
            templateChoice={templateChoice}
            setTemplateChoice={setTemplateChoice}
          />
          {analyzerId !== "new" && (
            <form
              onSubmit={(event) => {
                event.preventDefault();
              }}
            >
              <SearchSelect
                label="Tokenizer"
                value={analyzerQuery.data?.analyzer?.tokenizer?.id}
                useValueQuery={useTokenizerValueQuery}
                useOptionsQuery={useTokenizerOptionsQuery}
                useChangeMutation={useBindTokenizerToAnalyzerMutation}
                mapValueToMutationVariables={(tokenizerId) => ({ analyzerId, tokenizerId })}
                useRemoveMutation={useUnbindnTokenizerToAnalyzerMutation}
                mapValueToRemoveMutationVariables={() => ({ analyzerId })}
                invalidate={() => analyzerQuery.refetch()}
                description={"Tokenizer associated to Analyzer"}
              />
            </form>
          )}
          <div className="sheet-footer">
            <CustomButtom nameButton={analyzerId === "new" ? "Create" : "Update"} canSubmit={!form.canSubmit} typeSelectet="submit" />
          </div>
        </form>
      </ContainerFluid>
    </>
  );
}

export const tokenizerOptions = gql`
  query TokenizerOptions($searchText: String, $cursor: String) {
    options: tokenizers(searchText: $searchText, first: 5, after: $cursor) {
      edges {
        node {
          id
          name
          description
        }
      }
      pageInfo {
        hasNextPage
        endCursor
      }
    }
  }
`;
gql`
  query TokenizerValue($id: ID!) {
    value: tokenizer(id: $id) {
      id
      name
      description
    }
  }
`;
gql`
  mutation BindTokenizerToAnalyzer($analyzerId: ID!, $tokenizerId: ID!) {
    bindTokenizerToAnalyzer(analyzerId: $analyzerId, tokenizerId: $tokenizerId) {
      left {
        id
        tokenizer {
          id
        }
      }
      right {
        id
      }
    }
  }
`;

gql`
  mutation UnbindnTokenizerToAnalyzer($analyzerId: ID!) {
    unbindTokenizerFromAnalyzer(analyzerId: $analyzerId) {
      left {
        id
      }
    }
  }
`;

const TemplateAnalyzers = [
  {
    title: "select-type",
    description: "custom analyzer.",
    Json: `
  {
    "type": "select-type"
  }`,
    descriptionAttribute: `
  {
    
  }`,
    visible: "false",
  },
  {
    title: "custom",
    description: "custom analyzer.",
    Json: `
  {
    "type": "custom"
  }`,
    descriptionAttribute: `
  {
    
  }`,
    visible: "false",
  },
  {
    title: "fingerprint",
    description:
      "The fingerprint analyzer implements a fingerprinting algorithm which is used by the OpenRefine project to assist in clustering.",
    Json: `
    {
      "type": "fingerprint",
      "separator":" ",
      "max_output_size":255,
      "stopwords":"_none_",
      "stopwords_path":"path"
    }`,
    descriptionAttribute: `
    {
      "separator":"The character to use to concatenate the terms. Defaults to a space.",
      "max_output_size":"The maximum token size to emit. Defaults to 255. Tokens larger than this size will be discarded.",
      "stopwords":"A pre-defined stop words list like _english_ or an array containing a list of stop words. Defaults to _none_.",
      "stopwords_path":"The path to a file containing stop words."
    }`,
    visible: "false",
  },
  {
    title: "keyword",
    description: "The keyword analyzer is a “noop” analyzer which returns the entire input string as a single token.",
    Json: `
    {
      "type": "keyword"
    }`,
    descriptionAttribute: `
    {
    }`,
    visible: "false",
  },
  {
    title: "language",
    description: "A set of analyzers aimed at analyzing specific language text.",
    Json: `
    {
      "type": "language"
    }`,
    descriptionAttribute: `
    {
    }`,
    visible: "false",
  },
  {
    title: "simple",
    description:
      "The simple analyzer breaks text into tokens at any non-letter character, such as numbers, spaces, hyphens and apostrophes, discards non-letter characters, and changes uppercase to lowercase.",
    Json: `
    {
      "type": "simple"
    }`,
    visible: "false",
    descriptionAttribute: `
    {
    }`,
  },

  {
    title: "standard",
    description: "The standard analyzer is the default analyzer which is used if none is specified.",
    Json: `
    {
      "type": "standard",
      "max_token_length":255,
      "stopwords":"_none_",
      "stopwords_path":""
    }`,
    visible: "false",
    descriptionAttribute: `
    {
      "max_token_length":"The maximum token length. If a token is seen that exceeds this length then it is split at max_token_length intervals. Defaults to 255.",
      "stopwords":"A pre-defined stop words list like _english_ or an array containing a list of stop words. Defaults to _none_.",
      "stopwords_path":"The path to a file containing stop words."
    }`,
  },
  {
    title: "stop",
    description:
      "The stop analyzer is the same as the simple analyzer but adds support for removing stop words. It defaults to using the _english_ stop words.",
    Json: `
    {
      "type": "stop",
      "stopwords":"none",
      "stopwords_path":""
    }`,
    visible: "false",
    descriptionAttribute: `
    {
        "stopwords":"A pre-defined stop words list like _english_ or an array containing a list of stop words. Defaults to _english_.",
        "stopwords_path":"The path to a file containing stop words. This path is relative to the Elasticsearch config directory."
    }`,
  },
  {
    title: "whitespace",
    description: "The whitespace analyzer breaks text into terms whenever it encounters a whitespace character.",
    Json: `
    {
      "type": "whitespace"
     }`,
    visible: "false",
    descriptionAttribute: `
    {
        }`,
  },
];
