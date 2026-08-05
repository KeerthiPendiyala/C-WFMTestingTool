import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import type { RequirementGenerationResult } from '../api/client';
import { RequirementGenerationPanel } from './RequirementGenerationPanel';
import { validateRequirementDocument } from './requirementGenerationValidation';

const successfulResult: RequirementGenerationResult = {
  jobId: 'job-1',
  documentName: 'workforce-requirements.pdf',
  generatedRequirementCount: 12
};

function renderPanel(
  onGenerate: Parameters<typeof RequirementGenerationPanel>[0]['onGenerate'],
  scope = { projectId: 'project-1', suiteAssignmentId: 'suite-1', cycleId: 'cycle-1' }
) {
  return render(
    <RequirementGenerationPanel
      {...scope}
      authorized
      onGenerate={onGenerate}
      onViewGeneratedRequirements={vi.fn()}
    />
  );
}

describe('RequirementGenerationPanel', () => {
  it('validates supported documents and the 25 MB limit', () => {
    expect(validateRequirementDocument(new File(['content'], 'requirements.pdf'))).toBeNull();
    expect(validateRequirementDocument(new File(['content'], 'requirements.exe'))).toMatch(
      /PDF, DOCX, DOC, or CSV/i
    );
    expect(validateRequirementDocument(new File([], 'requirements.csv'))).toMatch(/empty/i);
    expect(
      validateRequirementDocument(
        new File([new Uint8Array(25 * 1024 * 1024 + 1)], 'requirements.docx')
      )
    ).toMatch(/25 MB/i);
  });

  it('shows the selected document without generation options and gates generation by scope', async () => {
    const user = userEvent.setup();
    const onGenerate = vi.fn(() => Promise.resolve(successfulResult));
    const { rerender } = renderPanel(onGenerate, {
      projectId: 'project-1',
      suiteAssignmentId: '',
      cycleId: ''
    });

    await user.upload(
      screen.getByTestId('requirement-document-input'),
      new File([new Uint8Array(2048)], 'workforce-requirements.pdf', {
        type: 'application/pdf'
      })
    );

    expect(screen.getByText('Selected Document')).toBeInTheDocument();
    expect(screen.getByText('workforce-requirements.pdf')).toBeInTheDocument();
    expect(screen.getByText('PDF')).toBeInTheDocument();
    expect(screen.getByText('2.0 KB')).toBeInTheDocument();
    expect(screen.queryByText('Requirement Generation Options')).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Generate Requirements using AI' })).toBeDisabled();

    rerender(
      <RequirementGenerationPanel
        projectId="project-1"
        suiteAssignmentId="suite-1"
        cycleId="cycle-1"
        authorized
        onGenerate={onGenerate}
        onViewGeneratedRequirements={vi.fn()}
      />
    );

    expect(screen.getByRole('button', { name: 'Generate Requirements using AI' })).toBeEnabled();
  });

  it('displays processing stages and the generation result', async () => {
    const user = userEvent.setup();
    let resolveGeneration: ((result: RequirementGenerationResult) => void) | undefined;
    const onGenerate = vi.fn(
      () =>
        new Promise<RequirementGenerationResult>((resolve) => {
          resolveGeneration = resolve;
        })
    );
    renderPanel(onGenerate);

    await user.upload(
      screen.getByTestId('requirement-document-input'),
      new File(['content'], 'workforce-requirements.pdf', { type: 'application/pdf' })
    );
    await user.click(screen.getByRole('button', { name: 'Generate Requirements using AI' }));

    expect(screen.getByRole('heading', { name: 'Generating requirements' })).toBeInTheDocument();
    for (const stage of ['Uploading', 'Extracting', 'Generating with AI', 'Validating', 'Saving']) {
      expect(screen.getByText(stage)).toBeInTheDocument();
    }

    act(() => {
      resolveGeneration?.(successfulResult);
    });

    expect(
      await screen.findByRole('heading', { name: 'Requirements generated successfully' })
    ).toBeInTheDocument();
    expect(screen.getByText(/12 requirements were generated/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'View Requirements' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Generate Again' })).toBeInTheDocument();
    expect(onGenerate).toHaveBeenCalledWith(expect.any(File));
  });

  it('shows a retry action after a generation failure', async () => {
    const user = userEvent.setup();
    const onGenerate = vi
      .fn()
      .mockRejectedValueOnce(new Error('Service unavailable'))
      .mockResolvedValueOnce(successfulResult);
    renderPanel(onGenerate);

    await user.upload(
      screen.getByTestId('requirement-document-input'),
      new File(['content'], 'requirements.csv', { type: 'text/csv' })
    );
    await user.click(screen.getByRole('button', { name: 'Generate Requirements using AI' }));

    expect(await screen.findByText('Service unavailable')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Retry' }));

    expect(
      await screen.findByRole('heading', { name: 'Requirements generated successfully' })
    ).toBeInTheDocument();
    expect(onGenerate).toHaveBeenCalledTimes(2);
  });
});
