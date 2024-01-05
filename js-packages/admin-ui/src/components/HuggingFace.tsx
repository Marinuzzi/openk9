import ClayButton from "@clayui/button";
import React from "react";
import { Spacy } from "../wizards/Logo/Spacy";
import { Button, useModal } from "@clayui/core";
import { Trasformers } from "../wizards/Logo/Trasformers";
import { Fastai } from "../wizards/Logo/Fastai";
import { Flair } from "../wizards/Logo/Flair";
import { Stanza } from "../wizards/Logo/Stanza";
import { useNavigate } from "react-router-dom";
import { useToast } from "./ToastProvider";
import { ClassNameButton } from "../App";
import { ContainerFluid, SimpleModal } from "./Form";
import { keycloak } from "./authentication";
import { useRestClient } from "./queryClient";
import { useMutation } from "@tanstack/react-query";

export function HuggingFace() {
  const [name, setName] = React.useState("");
  const [task, setTask] = React.useState("");
  const [library, setLibrary] = React.useState("");
  const [isInvalidateName, setIsInvalidateName] = React.useState(false);
  const [isInvalidateTask, setIsInvalidateTask] = React.useState(false);
  const [isInvalidateLibrary, setIsInvalidateLibrary] = React.useState(false);
  const { observer, onOpenChange, open } = useModal();
  const showToast = useToast();
  const navigate = useNavigate();
  const restClient = useRestClient();

  const mutation = useMutation(
    async () => {
      return await restClient.mlk8SResource.postApiK8SClientK8SDeployMlModel({
        library,
        modelName: name,
        pipelineName: task,
        tokenizerName: "string",
      });
    },
    {
      onSuccess: (data) => {
        showToast({ displayType: "info", title: "Model deployed", content: data.message });
      },
      onError: (error) => {
        showToast({ displayType: "info", title: "Error deploying model", content: "error" });
      },
    }
  );
  return (
    <React.Fragment>
      {open && (
        <SimpleModal
          observer={observer}
          labelContinue={"yes"}
          labelCancel={"cancel"}
          actionContinue={() => {
            mutation.mutate();
            onOpenChange(false);
            navigate(`/maching-learning/hugging-face-view`, { replace: true });
          }}
          actionCancel={() => {
            onOpenChange(false);
          }}
          description="Are you sure you want to release it?"
        />
      )}

      <ContainerFluid>
        <form
          className="sheet"
          onSubmit={(event) => {
            if (name !== "" && library !== "" && task !== "") {
              event.preventDefault();
              onOpenChange(true);
            } else {
              event.preventDefault();
              if (name === "") {
                setIsInvalidateName(true);
                setTimeout(() => {
                  setIsInvalidateName(false);
                }, 3000);
              }
              if (task === "") {
                setIsInvalidateTask(true);
                setTimeout(() => {
                  setIsInvalidateTask(false);
                }, 3000);
              }
              if (library === "") {
                setIsInvalidateLibrary(true);
                setTimeout(() => {
                  setIsInvalidateLibrary(false);
                }, 3000);
              }
            }
          }}
        >
          <div className="form-group-item">
            <label style={{ paddingTop: "18px" }}>Name</label>
            <input
              type="text"
              className="form-control"
              value={name}
              onChange={(event) => {
                setName(event.currentTarget.value);
              }}
            ></input>
            {isInvalidateName && (
              <div className="form-feedback-group has-warning">
                <div className="form-feedback-item">{"must not be empty"}</div>
              </div>
            )}
          </div>
          <div className="form-group-item">
            <label style={{ paddingTop: "18px" }}>Library:</label>
            <div>
              <Button
                displayType={null}
                className={library === "transformers-pytorch" ? "btn-info" : ""}
                onClick={(event) => {
                  if (library === "transformers-pytorch") {
                    setLibrary("");
                  } else {
                    setLibrary("transformers-pytorch");
                  }
                }}
              >
                <Trasformers />
                <span style={{ marginLeft: "10px" }}> Transformers with Pytorch</span>
              </Button>
              <Button
                displayType={null}
                className={library === "transformers-tensorflow" ? "btn-info" : ""}
                onClick={(event) => {
                  if (library === "transformers-tensorflow") {
                    setLibrary("");
                  } else {
                    setLibrary("transformers-tensorflow");
                  }
                }}
              >
                <Trasformers />
                <span style={{ marginLeft: "10px" }}> Transformers with Tensorflow</span>
              </Button>
              <Button
                displayType={null}
                className={library === "flair" ? "btn-info" : ""}
                onClick={(event) => {
                  if (library === "flair") {
                    setLibrary("");
                  } else {
                    setLibrary("flair");
                  }
                }}
              >
                <Flair />
                Flair
              </Button>
              <Button
                displayType={null}
                className={library === "spacy" ? "btn-info" : ""}
                onClick={(event) => {
                  if (library === "spacy") {
                    setLibrary("");
                  } else {
                    setLibrary("spacy");
                  }
                }}
              >
                <Spacy />
                Spacy
              </Button>
              <Button
                displayType={null}
                className={library === "stanza" ? "btn-info" : ""}
                onClick={(event) => {
                  if (library === "stanza") {
                    setLibrary("");
                  } else {
                    setLibrary("stanza");
                  }
                }}
              >
                <Stanza />
                Stanza
              </Button>
              <Button
                displayType={null}
                className={library === "fastai" ? "btn-info" : ""}
                onClick={(event) => {
                  if (library === "fastai") {
                    setLibrary("");
                  } else {
                    setLibrary("fastai");
                  }
                }}
              >
                <Fastai />
                Fastai
              </Button>
            </div>
          </div>
          {isInvalidateLibrary && (
            <div className="form-feedback-group has-warning">
              <div className="form-feedback-item">{"please choose library"}</div>
            </div>
          )}
          <div className="form-group-item">
            <label style={{ paddingTop: "18px" }}>Task</label>
            <select
              defaultValue={task}
              className={`clay-select form-control ${library === "task" ? "btn-info" : ""}`}
              onChange={(event) => {
                setTask(event.currentTarget.value);
              }}
            >
              <option className="clay-dropdown-item" value="">
                select task
              </option>
              <option className="clay-dropdown-item" value="text-classification">
                text classification
              </option>
              <option className="clay-dropdown-item" value="token-classification">
                token classification
              </option>
              <option className="clay-dropdown-item" value="object-detection">
                object detection
              </option>
              <option className="clay-dropdown-item" value="video-classification">
                video classification
              </option>
              <option className="clay-dropdown-item" value="image-classification">
                image classification
              </option>
              <option className="clay-dropdown-item" value="summarization">
                summarization
              </option>
              <option className="clay-dropdown-item" value="translation">
                translation
              </option>
              <option className="clay-dropdown-item" value="audio-classification">
                audio classification
              </option>
              <option className="clay-dropdown-item" value="automatic-speech-recognition">
                automatic speech recognition
              </option>
            </select>
          </div>
          {isInvalidateTask && (
            <div className="form-feedback-group has-warning">
              <div className="form-feedback-item">{"please choose task"}</div>
            </div>
          )}
          <div className="sheet-footer">
            <ClayButton className={ClassNameButton} type="submit">
              Deploy
            </ClayButton>
          </div>
        </form>
      </ContainerFluid>
    </React.Fragment>
  );
}
