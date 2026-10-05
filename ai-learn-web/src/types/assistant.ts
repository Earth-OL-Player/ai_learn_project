export type CommunityKind = 'comment' | 'reply' | 'suggestion';

export interface AssistantMessage {
  role: 'user' | 'assistant';
  content: string;
  sources?: Array<{ id: string; title: string; path: string }>;
}

export interface AssistantRun {
  id: string;
  sessionId: string;
  status: string;
  modelName: string;
  executionEpoch: number;
  clientInstanceId: string;
}

export interface AssistantPayload {
  pageKey?: string;
  tab?: string;
  kind?: CommunityKind;
  content?: string;
  parentId?: string;
  type?: string;
}

export interface AssistantOperation {
  id: string;
  type: string;
  status: string;
  payload: AssistantPayload;
  payloadVersion: number;
  payloadHash: string;
  authorized: boolean;
  nextStep: number;
  steps: string[];
  result?: AssistantPublishResult;
}

export interface AssistantSnapshot {
  run: AssistantRun;
  operations: AssistantOperation[];
}

export interface AssistantActionRequest {
  operationId: string;
  clientInstanceId: string;
  executionEpoch: number;
  payloadVersion: number;
  payloadHash: string;
  stepIndex: number;
  status: string;
  reason?: string;
}

export interface AssistantPublishResult {
  success: boolean;
  message: string;
  kind?: CommunityKind;
  record?: { id: string; [key: string]: unknown };
}

export interface AssistantSubmitContext {
  runId: string;
  request: AssistantActionRequest;
}
