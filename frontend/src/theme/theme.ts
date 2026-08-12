import { createTheme } from '@mui/material/styles';

import { designTokens } from './tokens';

export const appTheme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: designTokens.color.brand,
      light: '#32b563',
      dark: designTokens.color.brandDark,
      contrastText: '#ffffff'
    },
    secondary: {
      main: designTokens.color.accent
    },
    error: {
      main: designTokens.color.danger
    },
    success: {
      main: designTokens.color.brand,
      dark: designTokens.color.brandDark,
      contrastText: '#ffffff'
    },
    warning: {
      main: designTokens.color.warning
    },
    background: {
      default: designTokens.color.brandSurface,
      paper: '#ffffff'
    },
    text: {
      primary: designTokens.color.text,
      secondary: designTokens.color.muted
    }
  },
  shape: {
    borderRadius: 8
  },
  typography: {
    fontFamily: 'Roboto, Arial, sans-serif',
    h1: {
      letterSpacing: 0
    },
    h2: {
      letterSpacing: 0
    },
    h3: {
      letterSpacing: 0
    },
    h4: {
      letterSpacing: 0
    },
    h5: {
      letterSpacing: 0
    },
    h6: {
      letterSpacing: 0
    }
  },
  components: {
    MuiButton: {
      defaultProps: {
        disableElevation: true
      },
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: 700,
          borderRadius: 7
        }
      }
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderColor: designTokens.color.border,
          boxShadow: '0 2px 8px rgba(18, 32, 58, 0.07)'
        }
      }
    },
    MuiChip: {
      styleOverrides: {
        root: {
          fontWeight: 600
        }
      }
    },
    MuiCssBaseline: {
      styleOverrides: {
        ':focus-visible': {
          outline: `3px solid ${designTokens.color.accent}`,
          outlineOffset: '2px'
        },
        body: {
          minWidth: '320px',
          backgroundColor: designTokens.color.brandSurface
        }
      }
    },
    MuiOutlinedInput: {
      styleOverrides: {
        notchedOutline: {
          borderColor: designTokens.color.border
        }
      }
    },
    MuiTableCell: {
      styleOverrides: {
        head: {
          fontWeight: 500,
          color: designTokens.color.text,
          backgroundColor: '#f7f9f8'
        }
      }
    }
  }
});
