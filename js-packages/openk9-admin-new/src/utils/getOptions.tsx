import { KeyValue } from "@components/Form";

type Params = { useQuery?: any; data?: any; queryKeyPath: string; accessKey?: "node"; variables?: KeyValue };
const pathObject = {
  node: "node",
};

export default function useOptions({ useQuery, data, queryKeyPath, accessKey = "node", variables }: Params) {
  const queryData = useQuery?.({ variables: { ...variables } }) || {};
  const sourceData = data?.data || queryData?.data;

  const value = extract({ object: sourceData, pathKey: queryKeyPath });

  const getOptions = (value: any) => {
    return (
      value?.map((item: any) => ({
        value: item?.id || extract({ object: item, pathKey: pathObject[accessKey] })?.id || "",
        label: item?.name || extract({ object: item, pathKey: pathObject[accessKey] })?.name || "",
      })) || []
    );
  };

  const OptionQuery = (sourceData && getOptions(value)) || [];

  return {
    OptionQuery,
  };
}

function extract({ object, pathKey }: { object: any; pathKey: string }) {
  return pathKey.split(".")?.reduce((obj, key) => obj?.[key], object);
}
