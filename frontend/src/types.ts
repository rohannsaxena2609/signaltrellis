export type Page<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
}

export type AuthResponse = {
  tokenType: string
  accessToken: string
  expiresInSeconds: number
  userId: string
  workspaceId: string
  email: string
}

export type Profile = { userId: string; workspaceId: string; email: string }

export type Signal = {
  id: string
  type: string
  priority: 'HIGH' | 'MEDIUM' | 'LOW'
  status: 'ACTIVE' | 'DISMISSED' | 'RESOLVED'
  title: string
  explanation: string
  detectedAt: string
  lastDetectedAt: string
}

export type SignalEvidence = {
  id: string
  type: string
  eventId?: string
  commitmentId?: string
  summary: string
  observedValue?: string
}

export type Commitment = {
  id: string
  entityId?: string
  title: string
  amount?: number
  currency?: string
  dueDate: string
  status: 'OPEN' | 'SETTLED' | 'CANCELLED'
  notes?: string
}

export type DocumentRecord = {
  id: string
  sourceItemId: string
  filename: string
  contentType: string
  sizeBytes: number
  category: string
  processingStatus: 'UPLOADED' | 'QUEUED' | 'PROCESSING' | 'PROCESSED' | 'FAILED'
  createdAt: string
}

export type ExtractedFact = {
  id: string
  type: string
  extractedValue: string
  sourcePage: number
  sourceExcerpt: string
  confidence?: number
  reviewStatus: 'PENDING' | 'CONFIRMED' | 'CORRECTED' | 'REJECTED'
  correctedValue?: string
}

export type Extraction = {
  id: string
  documentId: string
  status: string
  extractor: string
  extractedCharacterCount: number
  aiStatus: string
  pages: { pageNumber: number; text: string; characterCount: number; extractionMethod: string }[]
  candidates: ExtractedFact[]
}

export type ProcessingJob = {
  jobId: string
  documentId: string
  status: DocumentRecord['processingStatus']
  attemptCount: number
  maxAttempts: number
  nextAttemptAt: string
  lastError?: string
}

export type ActionProposal = {
  id: string
  signalId?: string
  type: string
  title: string
  details?: string
  scheduledFor: string
  expiresAt: string
  status: 'PENDING_APPROVAL' | 'APPROVED' | 'REJECTED' | 'EXPIRED'
}

export type Entity = {
  id: string
  type: string
  name: string
  description?: string
  createdAt: string
}

export type Graph = {
  nodes: Entity[]
  relationships: { id: string; sourceEntityId: string; sourceName: string; targetEntityId: string; targetName: string; type: string }[]
  truncated: boolean
}

export type Citation = {
  reference: number
  documentId: string
  filename: string
  pageNumber: number
  chunkIndex: number
  startCharacter: number
  endCharacter: number
  text: string
}

export type AssistantResponse = {
  answer: string
  model: string
  actionPolicy: string
}
