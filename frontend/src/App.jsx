import { useState } from "react";
import "./App.css";

// =========================================================
// BACKEND URL
// =========================================================
// Replace this with your Render backend URL.
// Example:
// const API_URL = "https://docextract.onrender.com";
//
// Do NOT add /api/documents here.
const API_URL = "https://docextract-backend-a1n1.onrender.com";

function App() {
  const [file, setFile] = useState(null);
  const [result, setResult] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [error, setError] = useState("");

  // =========================================================
  // FILE SELECTION
  // =========================================================

  const handleFileChange = (event) => {
    const selectedFile = event.target.files[0];

    if (!selectedFile) {
      setFile(null);
      setResult(null);
      setError("");
      return;
    }

    setFile(selectedFile);
    setResult(null);
    setError("");
  };

  // =========================================================
  // UPLOAD DOCUMENT
  // =========================================================

  const handleUpload = async () => {
    if (!file) {
      setError("Please select a document first.");
      return;
    }

    setLoading(true);
    setError("");
    setResult(null);

    const formData = new FormData();
    formData.append("file", file);

    try {
      const response = await fetch(
        `${API_URL}/api/documents/upload`,
        {
          method: "POST",
          body: formData,
        }
      );

      if (!response.ok) {
        let message = "Upload failed.";

        try {
          const errorData = await response.json();

          if (errorData.message) {
            message = errorData.message;
          }
        } catch {
          // Ignore JSON parsing error
        }

        throw new Error(message);
      }

      const data = await response.json();

      setResult(data);

      if (showHistory) {
        loadDocuments();
      }
    } catch (err) {
      console.error("Upload error:", err);

      if (err.message === "Failed to fetch") {
        setError(
          "Could not connect to the backend. Please check the Render backend URL and make sure the server is running."
        );
      } else {
        setError(err.message || "Document upload failed.");
      }
    } finally {
      setLoading(false);
    }
  };

  // =========================================================
  // LOAD DOCUMENT HISTORY
  // =========================================================

  const loadDocuments = async () => {
    setHistoryLoading(true);

    try {
      const response = await fetch(
        `${API_URL}/api/documents`
      );

      if (!response.ok) {
        throw new Error("Could not load documents.");
      }

      const data = await response.json();

      setDocuments(data);
    } catch (err) {
      console.error("History error:", err);

      setError(
        "Could not load document history."
      );
    } finally {
      setHistoryLoading(false);
    }
  };

  // =========================================================
  // HISTORY
  // =========================================================

  const openHistory = () => {
    setShowHistory(true);
    loadDocuments();
  };

  const closeHistory = () => {
    setShowHistory(false);
  };

  // =========================================================
  // VALIDATION STATUS
  // =========================================================

  const getValidationStatus = () => {
    if (!result || !result.fields) {
      return null;
    }

    const errors = Object.keys(result.fields).filter(
      (key) => key.endsWith("Error")
    );

    return errors.length === 0;
  };

  // =========================================================
  // FIELD DISPLAY NAMES
  // =========================================================

  const formatFieldName = (key) => {
    const names = {
      name: "Name",
      fatherName: "Father Name",
      dob: "DOB",
      dateOfBirth: "Date of Birth",
      gender: "Gender",
      aadhaarNumber: "Aadhaar Number",
      panNumber: "PAN Number",
    };

    return (
      names[key] ||
      key
        .replace(/([A-Z])/g, " $1")
        .replace(/^./, (letter) =>
          letter.toUpperCase()
        )
    );
  };

  // =========================================================
  // EXTRACTED TEXT
  // =========================================================

  const getExtractedText = () => {
    if (!result || !result.fields) {
      return "";
    }

    const fields = result.fields;
    const lines = [];

    // =======================================================
    // PAN
    // =======================================================

    if (result.documentType === "PAN") {
      if (fields.name) {
        lines.push(`Name: ${fields.name}`);
      }

      if (fields.fatherName) {
        lines.push(
          `Father Name: ${fields.fatherName}`
        );
      }

      if (fields.dob) {
        lines.push(`DOB: ${fields.dob}`);
      }

      if (fields.panNumber) {
        lines.push(
          `PAN Number: ${fields.panNumber}`
        );
      }

      return lines.join("\n");
    }

    // =======================================================
    // AADHAAR
    // =======================================================

    if (result.documentType === "AADHAAR") {
      if (fields.name) {
        lines.push(`Name: ${fields.name}`);
      }

      if (fields.dob) {
        lines.push(`DOB: ${fields.dob}`);
      }

      if (fields.gender) {
        lines.push(`Gender: ${fields.gender}`);
      }

      if (fields.aadhaarNumber) {
        lines.push(
          `Aadhaar Number: ${fields.aadhaarNumber}`
        );
      }

      return lines.join("\n");
    }

    // =======================================================
    // OTHER DOCUMENT TYPES
    // =======================================================

    Object.entries(fields)
      .filter(([key]) => !key.endsWith("Error"))
      .forEach(([key, value]) => {
        lines.push(
          `${formatFieldName(key)}: ${value}`
        );
      });

    return lines.join("\n");
  };

  // =========================================================
  // RESET
  // =========================================================

  const handleNewDocument = () => {
    setFile(null);
    setResult(null);
    setError("");

    const fileInput =
      document.getElementById("document-file");

    if (fileInput) {
      fileInput.value = "";
    }
  };

  return (
    <div className="app">

      {/* =====================================================
          HEADER
      ===================================================== */}

      <header className="top-header">

        <div className="header-content">

          <div className="brand">

            <div className="brand-icon">
              D
            </div>

            <div>
              <h1>DocExtract</h1>

              <p>
                Document Extraction & Validation
              </p>
            </div>

          </div>

          <button
            className="history-top-button"
            onClick={openHistory}
          >
            <span>☰</span>
            Document History
          </button>

        </div>

      </header>

      {/* =====================================================
          MAIN
      ===================================================== */}

      <main>

        {/* ===================================================
            WELCOME
        =================================================== */}

        {!result && (
          <section className="welcome-section">

            <h2>
              Extract information from your documents
            </h2>

            <p>
              Upload a document and DocExtract will
              automatically extract important information
              and validate the data.
            </p>

          </section>
        )}

        {/* ===================================================
            UPLOAD CARD
        =================================================== */}

        <section className="upload-card">

          <div className="upload-icon">
            📄
          </div>

          <h2>
            Upload Document
          </h2>

          <p className="upload-description">
            Select a PDF, image or text document to begin
            extraction.
          </p>

          <label className="file-upload-area">

            <input
              id="document-file"
              type="file"
              onChange={handleFileChange}
              accept=".pdf,.txt,.png,.jpg,.jpeg,.docx"
            />

            <span className="upload-symbol">
              ↑
            </span>

            <strong>
              {file
                ? file.name
                : "Choose a document"}
            </strong>

            <small>
              PDF, DOCX, TXT, PNG, JPG or JPEG files
            </small>

          </label>

          {/* SELECTED FILE */}

          {file && (
            <div className="selected-file">

              <span>
                📎
              </span>

              <span>
                {file.name}
              </span>

              <span className="file-ready">
                Ready
              </span>

            </div>
          )}

          {/* UPLOAD BUTTON */}

          <button
            className="upload-button"
            onClick={handleUpload}
            disabled={loading}
          >

            {loading ? (
              <>
                <span className="spinner"></span>
                Processing Document...
              </>
            ) : (
              "Upload & Extract"
            )}

          </button>

          {/* ERROR */}

          {error && (
            <div className="error">
              {error}
            </div>
          )}

        </section>

        {/* ===================================================
            RESULT
        =================================================== */}

        {result && (
          <section className="result-section">

            {/* ===============================================
                VALIDATION BANNER
            =============================================== */}

            {result.documentType !== "UNKNOWN" && (
              <div
                className={
                  getValidationStatus()
                    ? "validation-banner valid"
                    : "validation-banner invalid"
                }
              >

                <div className="validation-icon">

                  {getValidationStatus()
                    ? "✓"
                    : "!"}

                </div>

                <div>

                  <strong>
                    {getValidationStatus()
                      ? "Document Valid"
                      : "Document Invalid"}
                  </strong>

                  <p>
                    {getValidationStatus()
                      ? "All extracted fields passed validation."
                      : "One or more extracted fields failed validation."}
                  </p>

                </div>

              </div>
            )}

            {/* ===============================================
                DOCUMENT INFORMATION
            =============================================== */}

            <div className="section-card">

              <div className="section-title">

                <div>

                  <h2>
                    Document Information
                  </h2>

                  <p>
                    Details about the uploaded document
                  </p>

                </div>

              </div>

              <div className="document-info-grid">

                <div className="document-info-item">

                  <span>
                    File Name
                  </span>

                  <strong>
                    {result.fileName}
                  </strong>

                </div>

                <div className="document-info-item">

                  <span>
                    File Type
                  </span>

                  <strong>
                    {result.fileType}
                  </strong>

                </div>

                <div className="document-info-item">

                  <span>
                    Document Type
                  </span>

                  <strong className="type-badge">
                    {result.documentType}
                  </strong>

                </div>

                <div className="document-info-item">

                  <span>
                    Document ID
                  </span>

                  <strong>
                    #{result.id}
                  </strong>

                </div>

              </div>

            </div>

            {/* ===============================================
                EXTRACTED FIELDS
            =============================================== */}

            <div className="section-card">

              <div className="section-title">

                <div>

                  <h2>
                    Extracted Information
                  </h2>

                  <p>
                    Information identified from the document
                  </p>

                </div>

              </div>

              {Object.keys(result.fields || {}).length > 0 ? (

                <div className="fields-list">

                  {/* NORMAL FIELDS */}

                  {Object.entries(result.fields)
                    .filter(
                      ([key]) =>
                        !key.endsWith("Error")
                    )
                    .map(([key, value]) => {

                      const errorKey =
                        `${key}Error`;

                      const hasError =
                        result.fields[errorKey];

                      return (
                        <div
                          className={
                            hasError
                              ? "field-item field-invalid"
                              : "field-item"
                          }
                          key={key}
                        >

                          <div className="field-label">
                            {formatFieldName(key)}
                          </div>

                          <div className="field-value">
                            {value}
                          </div>

                          <div
                            className={
                              hasError
                                ? "field-status invalid"
                                : "field-status valid"
                            }
                          >

                            {hasError
                              ? "✕ Invalid"
                              : "✓ Valid"}

                          </div>

                        </div>
                      );
                    })}

                  {/* VALIDATION ERRORS */}

                  {Object.entries(result.fields)
                    .filter(
                      ([key]) =>
                        key.endsWith("Error")
                    )
                    .map(([key, value]) => {

                      const fieldName =
                        key.replace("Error", "");

                      return (
                        <div
                          className="field-item field-invalid"
                          key={key}
                        >

                          <div className="field-label">
                            {formatFieldName(fieldName)}
                          </div>

                          <div className="field-value error-value">
                            {value}
                          </div>

                          <div className="field-status invalid">
                            ✕ Invalid
                          </div>

                        </div>
                      );
                    })}

                </div>

              ) : (
                <div className="no-fields">
                  No fields were extracted from this document.
                </div>
              )}

            </div>

            {/* ===============================================
                EXTRACTED TEXT
            =============================================== */}

            <div className="section-card">

              <div className="section-title">

                <div>

                  <h2>
                    Extracted Text
                  </h2>

                  <p>
                    Information extracted from the document
                  </p>

                </div>

              </div>

              <pre>
                {getExtractedText()}
              </pre>

            </div>

            {/* ===============================================
                NEW DOCUMENT BUTTON
            =============================================== */}

            <div className="new-document-container">

              <button
                className="upload-button"
                onClick={handleNewDocument}
              >
                Upload Another Document
              </button>

            </div>

          </section>
        )}

      </main>

      {/* =====================================================
          HISTORY MODAL
      ===================================================== */}

      {showHistory && (

        <div
          className="modal-overlay"
          onClick={closeHistory}
        >

          <div
            className="history-modal"
            onClick={(event) =>
              event.stopPropagation()
            }
          >

            <div className="history-modal-header">

              <div>

                <h2>
                  Document History
                </h2>

                <p>
                  Previously uploaded documents
                </p>

              </div>

              <button
                className="close-button"
                onClick={closeHistory}
              >
                ×
              </button>

            </div>

            {historyLoading ? (

              <div className="history-loading">
                Loading documents...
              </div>

            ) : documents.length > 0 ? (

              <div className="history-list">

                {documents.map((document) => (

                  <div
                    className="history-item"
                    key={document.id}
                  >

                    <div className="history-id">
                      #{document.id}
                    </div>

                    <div className="history-details">

                      <strong>
                        {document.fileName}
                      </strong>

                      <span>
                        {document.fileType}
                      </span>

                    </div>

                    <div className="history-type">
                      {document.documentType}
                    </div>

                  </div>

                ))}

              </div>

            ) : (

              <div className="empty-history">

                <div>
                  📂
                </div>

                <strong>
                  No documents found
                </strong>

                <p>
                  Uploaded documents will appear here.
                </p>

              </div>

            )}

          </div>

        </div>
      )}

      {/* =====================================================
          FOOTER
      ===================================================== */}

      <footer>

        <p>

          DocExtract

          <span>
            •
          </span>

          Document Extraction & Validation System

        </p>

      </footer>

    </div>
  );
}

export default App;

