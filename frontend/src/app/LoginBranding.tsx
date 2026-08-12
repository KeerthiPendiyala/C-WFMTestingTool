import AutoAwesomeOutlinedIcon from '@mui/icons-material/AutoAwesomeOutlined';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';
import SecurityOutlinedIcon from '@mui/icons-material/SecurityOutlined';
import { Box, Chip, Stack, Typography } from '@mui/material';
import { useState } from 'react';

const smartWfmBrand = {
  teal: '#32AA98',
  tealDark: '#137D72',
  lime: '#84B514',
  limeDark: '#66930B'
} as const;

function SmartWfmLogo() {
  const [logoUnavailable, setLogoUnavailable] = useState(false);

  if (logoUnavailable) {
    return (
      <Typography
        component="p"
        variant="h5"
        fontWeight={800}
        letterSpacing="0.08em"
        color={smartWfmBrand.limeDark}
      >
        SMART WFM
      </Typography>
    );
  }

  return (
    <Box
      component="img"
      src="/images/smartwfm-logo-official.png"
      alt="Smart WFM"
      onError={() => {
        setLogoUnavailable(true);
      }}
      sx={{
        width: '88%',
        maxWidth: 270,
        height: 'auto',
        display: 'block'
      }}
    />
  );
}

export function LoginBrandPanel({ appName }: { appName: string }) {
  return (
    <Stack
      component="section"
      aria-labelledby="smart-wfm-product-name"
      justifyContent="space-between"
      sx={{
        position: 'relative',
        isolation: 'isolate',
        overflow: 'hidden',
        minHeight: { xs: 300, md: '100vh' },
        px: { xs: 3, sm: 6, lg: 9 },
        py: { xs: 4, sm: 6, lg: 8 },
        color: 'common.white',
        background: `linear-gradient(145deg, ${smartWfmBrand.tealDark} 0%, ${smartWfmBrand.teal} 52%, ${smartWfmBrand.lime} 100%)`,
        '&::before': {
          content: '""',
          position: 'absolute',
          zIndex: -1,
          width: 420,
          height: 420,
          borderRadius: '50%',
          right: -170,
          top: -170,
          background: 'rgba(255, 255, 255, 0.1)'
        },
        '&::after': {
          content: '""',
          position: 'absolute',
          zIndex: -1,
          width: 300,
          height: 300,
          borderRadius: '50%',
          left: -150,
          bottom: -140,
          border: '54px solid rgba(255, 255, 255, 0.09)'
        }
      }}
    >
      <Box
        component="a"
        href="https://www.smartwfm.com/"
        target="_blank"
        rel="noopener noreferrer"
        aria-label="Visit the Smart WFM website"
        sx={{
          position: 'relative',
          width: { xs: 280, sm: 320 },
          height: { xs: 96, sm: 108 },
          bgcolor: 'common.white',
          borderRadius: 3,
          overflow: 'hidden',
          border: '1px solid rgba(255, 255, 255, 0.72)',
          boxShadow: '0 18px 44px rgba(12, 83, 74, 0.25)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          '&:focus-visible': {
            outline: '3px solid white',
            outlineOffset: 4
          }
        }}
      >
        <SmartWfmLogo />
      </Box>

      <Box sx={{ my: { xs: 4, md: 8 }, maxWidth: 560 }}>
        <Typography
          id="smart-wfm-product-name"
          component="h2"
          sx={{
            fontSize: { xs: '2rem', sm: '2.75rem', lg: '3.5rem' },
            lineHeight: 1.08,
            fontWeight: 800,
            letterSpacing: '-0.035em'
          }}
        >
          {appName}
        </Typography>
        <Typography
          sx={{
            mt: 2,
            color: '#F3FFD6',
            fontSize: { xs: '1.05rem', sm: '1.25rem' },
            fontWeight: 600
          }}
        >
          AI-Powered Workforce Management
        </Typography>
        <Typography
          sx={{
            mt: 3,
            maxWidth: 500,
            color: 'rgba(255, 255, 255, 0.88)',
            fontSize: '1rem',
            lineHeight: 1.75
          }}
        >
          Plan, manage, and deliver workforce-management quality with secure, enterprise-ready test
          operations.
        </Typography>
      </Box>

      <Stack
        direction="row"
        useFlexGap
        flexWrap="wrap"
        gap={1}
        sx={{ display: { xs: 'none', sm: 'flex' } }}
      >
        {[
          { icon: <SecurityOutlinedIcon />, label: 'Secure by design' },
          { icon: <AutoAwesomeOutlinedIcon />, label: 'AI assisted' },
          { icon: <CheckCircleOutlineIcon />, label: 'Accessible' }
        ].map((item) => (
          <Chip
            key={item.label}
            icon={item.icon}
            label={item.label}
            sx={{
              color: 'common.white',
              bgcolor: 'rgba(255, 255, 255, 0.13)',
              borderColor: 'rgba(255, 255, 255, 0.34)',
              '& .MuiChip-icon': { color: '#F3FFD6' }
            }}
            variant="outlined"
          />
        ))}
      </Stack>
    </Stack>
  );
}
