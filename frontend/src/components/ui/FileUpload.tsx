import React, { useState } from "react";

interface FileUploadProps {
  label: string;
  accept?: string;
  maxSizeMB?: number;
  value?: string;
  onChange: (file: File) => Promise<void>;
  onRemove?: () => void;
  disabled?: boolean;
  helpText?: string;
  required?: boolean;
  error?: string;
}


export default function FileUpload({
  label,
  accept = "image/*",
  maxSizeMB = 5,
  value,
  onChange,
  onRemove,
  disabled = false,
  helpText,
  required = false,
  error,
}: FileUploadProps) {
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    // Validate file type
    if (accept && !file.type.match(new RegExp(accept.replace("*", ".*")))) {
      setUploadError(`Please select a file of type: ${accept}`);
      return;
    }

    // Validate file size
    if (file.size > maxSizeMB * 1024 * 1024) {
      setUploadError(`File size must be less than ${maxSizeMB}MB`);
      return;
    }

    setUploading(true);
    setUploadError(null);

    try {
      await onChange(file);
    } catch (error) {
      console.error("Upload failed:", error);
      setUploadError("Failed to upload file. Please try again.");
    } finally {
      setUploading(false);
    }
  };

  return (
    <div>
      <label className="block text-sm font-medium text-gray-300 mb-2">
        {label} {required && <span className="text-red-400">*</span>}
      </label>

      <div className="space-y-4">
        {/* File Upload Input */}
        <div className="relative">
          <input
            type="file"
            accept={accept}
            onChange={handleFileChange}
            disabled={uploading || disabled}
            className="hidden"
            id={`file-upload-${label.replace(/\s+/g, "-").toLowerCase()}`}
          />
          <label
            htmlFor={`file-upload-${label.replace(/\s+/g, "-").toLowerCase()}`}
            className={`cursor-pointer inline-flex items-center px-4 py-2 border border-gray-600 rounded-lg text-sm font-medium text-gray-300 bg-gray-700 hover:bg-gray-600 transition-colors duration-200 ${
              uploading || disabled
                ? "opacity-50 cursor-not-allowed"
                : ""
            }`}
          >
            {uploading ? (
              <>
                <svg
                  className="animate-spin -ml-1 mr-2 h-4 w-4 text-white"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle
                    className="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    strokeWidth="4"
                  ></circle>
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  ></path>
                </svg>
                Uploading...
              </>
            ) : (
              <>
                <svg
                  className="w-4 h-4 mr-2"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
                  />
                </svg>
                Choose File
              </>
            )}
          </label>
        </div>

        {/* Upload Error */}
        {(error || uploadError) && (
          <p className="text-red-400 text-sm animate-in slide-in-from-top duration-300">
            {error || uploadError}
          </p>
        )}

        {/* Success Message */}
        {value && !error && !uploadError && (
          <div className="flex items-center justify-between text-green-400 text-sm animate-in slide-in-from-top duration-300">
            <div className="flex items-center">
              <svg
                className="w-4 h-4 mr-2"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M5 13l4 4L19 7"
                />
              </svg>
              File uploaded successfully
            </div>
            {onRemove && (
              <button
                type="button"
                onClick={onRemove}
                className="text-red-500 text-sm hover:text-red-400"
              >
                Remove
              </button>
            )}
          </div>
        )}

        {/* Help Text */}
        {helpText && (
          <p className="text-gray-500 text-sm">{helpText}</p>
        )}
      </div>
    </div>
  );
}

