import {
  InteractionRequiredAuthError,
  type AccountInfo,
  type IPublicClientApplication
} from '@azure/msal-browser';
import { useCallback, useState } from 'react';

import { loginRequest } from './config';

export type TokenState = 'idle' | 'acquiring' | 'expired' | 'claims_challenge' | 'access_denied';

export function useAccessToken(instance: IPublicClientApplication, account: AccountInfo | null) {
  const [state, setState] = useState<TokenState>('idle');

  const acquireAccessToken = useCallback(async () => {
    if (!account) {
      setState('expired');
      return null;
    }

    setState('acquiring');
    try {
      const result = await instance.acquireTokenSilent({
        ...loginRequest,
        account
      });
      setState('idle');
      return result.accessToken;
    } catch (error) {
      if (error instanceof InteractionRequiredAuthError) {
        setState('claims_challenge');
        const redirectRequest = {
          ...loginRequest,
          account
        };
        if (typeof error.claims === 'string') {
          Object.assign(redirectRequest, { claims: error.claims });
        }
        await instance.acquireTokenRedirect(redirectRequest);
        return null;
      }
      const message = error instanceof Error ? error.message.toLowerCase() : '';
      setState(message.includes('access_denied') ? 'access_denied' : 'expired');
      return null;
    }
  }, [account, instance]);

  return { acquireAccessToken, tokenState: state };
}
