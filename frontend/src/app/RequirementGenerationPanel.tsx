import AutoAwesomeOutlinedIcon from '@mui/icons-material/AutoAwesomeOutlined';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import ReplayOutlinedIcon from '@mui/icons-material/ReplayOutlined';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import WarningAmberOutlinedIcon from '@mui/icons-material/WarningAmberOutlined';
import {
  Alert,
  Button,
  Card,
  CardContent,
  Chip,
  CircularProgress,
  LinearProgress,
  Paper,
  Stack,
  Typography
} from '@mui/material';
import { useEffect, useMemo, useState } from 'react';

import type { RequirementGenerationResult } from '../api/client';
import { validateRequirementDocument } from './requirementGenerationValidation';

type GenerationPhase = 'idle' | 'processing' | 'success' | 'failure';

const processingStages = [
  'Uploading',
  'Extracting',
  'Generating with AI',
  'Validating',
  'Saving'
] as const;

export function RequirementGenerationPanel({
  projectId,
  suiteAssignmentId,
  cycleId,
  authorized,
  onGenerate,
  onViewGeneratedRequirements
}: {
  projectId: string;
  suiteAssignmentId: string;
  cycleId: string;
  authorized: boolean;
  onGenerate: (document: File) => Promise<RequirementGenerationResult>;
  onViewGeneratedRequirements: () => void;
}) {
  const [document, setDocument] = useState<File | null>(null);
  const [validationError, setValidationError] = useState<string | null>(null);
  const [phase, setPhase] = useState<GenerationPhase>('idle');
  const [activeStage, setActiveStage] = useState(0);
  const [result, setResult] = useState<RequirementGenerationResult | null>(null);
  const [generationError, setGenerationError] = useState<string | null>(null);

  useEffect(() => {
    if (phase !== 'processing') return;
    const timer = window.setInterval(() => {
      setActiveStage((current) => {
        return Math.min(current + 1, processingStages.length - 1);
      });
    }, 700);
    return () => {
      window.clearInterval(timer);
    };
  }, [phase]);

  const resetOutcome = () => {
    setPhase('idle');
    setActiveStage(0);
    setResult(null);
    setGenerationError(null);
  };

  const selectDocument = (selected: File | undefined) => {
    if (!selected) return;
    const error = validateRequirementDocument(selected);
    if (error) {
      setDocument(null);
      setValidationError(error);
      resetOutcome();
      return;
    }
    setDocument(selected);
    setValidationError(null);
    resetOutcome();
  };

  const generationEnabled = useMemo(
    () =>
      authorized &&
      Boolean(projectId && suiteAssignmentId && cycleId && document) &&
      !validationError,
    [authorized, cycleId, document, projectId, suiteAssignmentId, validationError]
  );

  const generate = async () => {
    if (!document || !generationEnabled) return;
    setPhase('processing');
    setActiveStage(0);
    setGenerationError(null);
    try {
      const generated = await onGenerate(document);
      setActiveStage(processingStages.length - 1);
      setResult(generated);
      setPhase('success');
    } catch (error) {
      setGenerationError(
        error instanceof Error && error.message.trim()
          ? error.message
          : 'Requirements could not be generated. Please try again.'
      );
      setPhase('failure');
    }
  };

  return (
    <Stack spacing={2}>
      <Card variant="outlined">
        <CardContent>
          <Stack spacing={2}>
            <Stack spacing={0.5}>
              <Typography variant="h6" component="h2" fontWeight={800}>
                Upload Requirement Document
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Upload a PDF, DOCX, DOC or CSV document. Maximum file size is 25 MB.
              </Typography>
            </Stack>

            {!document ? (
              <Button
                component="label"
                variant="outlined"
                startIcon={<UploadFileOutlinedIcon />}
                sx={{ alignSelf: 'flex-start' }}
              >
                Choose Document
                <input
                  hidden
                  type="file"
                  accept=".pdf,.docx,.doc,.csv"
                  data-testid="requirement-document-input"
                  onChange={(event) => {
                    selectDocument(event.target.files?.[0]);
                    event.target.value = '';
                  }}
                />
              </Button>
            ) : (
              <Paper variant="outlined" sx={{ p: 2 }}>
                <Stack
                  direction={{ xs: 'column', sm: 'row' }}
                  spacing={2}
                  alignItems={{ sm: 'center' }}
                  justifyContent="space-between"
                >
                  <Stack direction="row" spacing={1.5} alignItems="center" minWidth={0}>
                    <DescriptionOutlinedIcon color="primary" fontSize="large" />
                    <Stack minWidth={0}>
                      <Typography variant="overline" color="text.secondary">
                        Selected Document
                      </Typography>
                      <Typography fontWeight={700} noWrap title={document.name}>
                        {document.name}
                      </Typography>
                      <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
                        <Chip size="small" label={documentType(document)} />
                        <Chip
                          size="small"
                          variant="outlined"
                          label={formatFileSize(document.size)}
                        />
                      </Stack>
                    </Stack>
                  </Stack>
                  <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                    <Button
                      component="label"
                      variant="outlined"
                      startIcon={<UploadFileOutlinedIcon />}
                    >
                      Change File
                      <input
                        hidden
                        type="file"
                        accept=".pdf,.docx,.doc,.csv"
                        data-testid="change-requirement-document-input"
                        onChange={(event) => {
                          selectDocument(event.target.files?.[0]);
                          event.target.value = '';
                        }}
                      />
                    </Button>
                    <Button
                      color="error"
                      startIcon={<DeleteOutlineIcon />}
                      onClick={() => {
                        setDocument(null);
                        setValidationError(null);
                        resetOutcome();
                      }}
                    >
                      Remove
                    </Button>
                  </Stack>
                </Stack>
              </Paper>
            )}

            {validationError && <Alert severity="error">{validationError}</Alert>}
          </Stack>
        </CardContent>
      </Card>

      {phase === 'processing' && (
        <Card variant="outlined" aria-live="polite">
          <CardContent>
            <Stack spacing={2}>
              <Stack direction="row" spacing={1.5} alignItems="center">
                <CircularProgress size={24} />
                <Typography variant="h6" component="h2" fontWeight={800}>
                  Generating requirements
                </Typography>
              </Stack>
              <LinearProgress
                variant="determinate"
                value={((activeStage + 1) / processingStages.length) * 100}
                aria-label="Requirement generation progress"
              />
              <Stack direction={{ xs: 'column', md: 'row' }} spacing={1}>
                {processingStages.map((stage, index) => (
                  <Chip
                    key={stage}
                    {...(index < activeStage ? { icon: <CheckCircleOutlineIcon /> } : {})}
                    color={
                      index === activeStage
                        ? 'primary'
                        : index < activeStage
                          ? 'success'
                          : 'default'
                    }
                    variant={index <= activeStage ? 'filled' : 'outlined'}
                    label={stage}
                  />
                ))}
              </Stack>
            </Stack>
          </CardContent>
        </Card>
      )}

      {phase === 'success' && result && (
        <Card variant="outlined" sx={{ borderColor: 'success.light' }} aria-live="polite">
          <CardContent>
            <Stack spacing={2}>
              <Stack direction="row" spacing={1.5} alignItems="center">
                <CheckCircleOutlineIcon color="success" />
                <Stack>
                  <Typography variant="h6" component="h2" fontWeight={800}>
                    Requirements generated successfully
                  </Typography>
                  <Typography color="text.secondary">
                    {result.generatedRequirementCount} requirements were generated from{' '}
                    {result.documentName}.
                  </Typography>
                </Stack>
              </Stack>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
                <Button variant="contained" onClick={onViewGeneratedRequirements}>
                  Manage Requirements
                </Button>
                <Button
                  variant="outlined"
                  startIcon={<ReplayOutlinedIcon />}
                  onClick={resetOutcome}
                >
                  Generate Again
                </Button>
              </Stack>
            </Stack>
          </CardContent>
        </Card>
      )}

      {phase === 'failure' && (
        <Alert
          severity="error"
          icon={<WarningAmberOutlinedIcon />}
          action={
            <Button
              color="inherit"
              size="small"
              startIcon={<ReplayOutlinedIcon />}
              onClick={() => void generate()}
            >
              Retry
            </Button>
          }
        >
          {generationError}
        </Alert>
      )}

      {phase !== 'success' && (
        <Stack direction="row" justifyContent="flex-end">
          <Button
            variant="contained"
            size="large"
            startIcon={
              phase === 'processing' ? (
                <CircularProgress size={18} color="inherit" />
              ) : (
                <AutoAwesomeOutlinedIcon />
              )
            }
            disabled={!generationEnabled || phase === 'processing'}
            onClick={() => void generate()}
          >
            {phase === 'processing' ? 'Generating…' : 'Generate Requirements using AI'}
          </Button>
        </Stack>
      )}
    </Stack>
  );
}

function documentType(document: File): string {
  return document.name.split('.').pop()?.toUpperCase() ?? 'DOCUMENT';
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${String(bytes)} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
