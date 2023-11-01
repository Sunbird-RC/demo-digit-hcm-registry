const CERTIFICATE_CONTROLLER_ID = process.env.REACT_APP_CERTIFICATE_CONTROLLER_ID || 'https://digit.org/servicedelivery/';
const CERTIFICATE_ISSUER = process.env.CERTIFICATE_ISSUER || "https://digit.org";
const ENABLE_PDF_VERIFICATION = process.env.ENABLE_PDF_VERIFICATION || true ;
const DOCUMENT_LOADER_URL= process.env.DOCUMENT_LOADER_URL || "https://raw.githubusercontent.com/Sunbird-RC/demo-digit-hcm-registry/main/context/DIGITContext.json";

module.exports = {
  CERTIFICATE_CONTROLLER_ID,
  CERTIFICATE_ISSUER,
  ENABLE_PDF_VERIFICATION,
  DOCUMENT_LOADER_URL
};
