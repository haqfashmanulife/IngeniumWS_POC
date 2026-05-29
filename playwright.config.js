module.exports = {
  timeout: 180000,
  retries: 0,
  use: {
    headless: true,
    viewport: { width: 1920, height: 1080 },
    screenshot: 'only-on-failure',
    ignoreHTTPSErrors: true
  }
};
