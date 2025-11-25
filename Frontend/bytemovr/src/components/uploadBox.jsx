import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faUpload } from '@fortawesome/free-solid-svg-icons'
import { useState } from 'react'
import { PulseLoader } from "react-spinners"
import { useDropzone } from 'react-dropzone'

const API_BASE = 'http://api.bytemovr.com/api' // <- adjust to your backend

function UploadBox() {
    const [isUploading, setIsUploading] = useState(false)
    const [progress, setProgress] = useState(0)
    const [error, setError] = useState(null)
    const [downloadLink, setDownloadLink] = useState(null)

    // --- Backend helpers ---

    // 1) Tell backend "I want to upload this file"
    const startUpload = async (file) => {
        const res = await fetch(`${API_BASE}/files`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                fileName: file.name,
                sizeBytes: file.size,
                contentType: file.type || 'application/octet-stream',
            }),
        })

        if (!res.ok) {
            throw new Error('Failed to start upload')
        }

        // { id, uploadId, partSizeBytes, downloadId, expiresAt }
        return res.json()
    }

    // 2) Ask backend for presigned URLs for specific part numbers
    const getPartUrls = async (id, uploadId, partNumbers) => {
        const res = await fetch(`${API_BASE}/files/${id}/parts`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ uploadId, parts: partNumbers }),
        })

        if (!res.ok) {
            throw new Error('Failed to get part URLs')
        }

        const data = await res.json()
        // data.parts = [{ partNumber, uploadUrl }]
        return data.parts
    }

    // 3) Tell backend "all parts uploaded, complete the multipart upload"
    const completeUpload = async (id, uploadId, uploadedParts) => {
        const res = await fetch(`${API_BASE}/files/${id}/complete`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                uploadId,
                parts: uploadedParts, // [{ partNumber, etag }]
            }),
        })

        if (!res.ok) {
            throw new Error('Failed to complete upload')
        }

        // { id, downloadId }
        return res.json()
    }

    // --- Core multipart upload logic (frontend -> S3) ---

    const uploadFileInParts = async (file, id, uploadId, partSizeBytes) => {
        const totalParts = Math.ceil(file.size / partSizeBytes)
        const uploadedParts = [] // { partNumber, etag }

        for (let partNumber = 1; partNumber <= totalParts; partNumber++) {
            const start = (partNumber - 1) * partSizeBytes
            const end = Math.min(partNumber * partSizeBytes, file.size)
            const blobPart = file.slice(start, end)

            // Ask backend for presigned URL for this part
            const partsInfo = await getPartUrls(id, uploadId, [partNumber])
            const uploadUrl = partsInfo[0].uploadUrl

            // PUT part directly to S3
            const res = await fetch(uploadUrl, {
                method: 'PUT',
                body: blobPart,
            })

            if (!res.ok) {
                throw new Error(`Failed uploading part ${partNumber}`)
            }

            const etag = res.headers.get('ETag')
            if (!etag) {
                console.warn('No ETag returned for part', partNumber)
            }

            uploadedParts.push({ partNumber, etag })

            const pct = Math.round((partNumber / totalParts) * 100)
            setProgress(pct)
        }

        return uploadedParts
    }

    // --- Overall orchestrator for one dropped file ---

    const handleFileUpload = async (file) => {
        setIsUploading(true)
        setError(null)
        setDownloadLink(null)
        setProgress(0)

        try {
            // 1) Start upload with backend
            const { id, uploadId, partSizeBytes, downloadId } = await startUpload(file)

            // 2) Upload to S3 in parts
            const uploadedParts = await uploadFileInParts(file, id, uploadId, partSizeBytes)

            // 3) Tell backend we're done
            const completeData = await completeUpload(id, uploadId, uploadedParts)

            // 4) Build the one-time download link (using backend's download endpoint)
            const finalDownloadId = completeData.downloadId || downloadId || id
            let link = `${API_BASE.replace('/api', '')}/download/${finalDownloadId}`
            link = link.replace("44.200.84.114:8080", "api.bytemovr.com")
            setDownloadLink(link.replace("download", "d"))
        } catch (err) {
            console.error(err)
            setError(err.message || 'Upload failed')
        } finally {
            setIsUploading(false)
        }
    }

    // --- Dropzone ---

    const onDrop = (acceptedFiles) => {
        if (!acceptedFiles || acceptedFiles.length === 0) return
        const file = acceptedFiles[0]
        handleFileUpload(file)
    }

    const { getRootProps, getInputProps, isDragActive } = useDropzone({
        onDrop,
        multiple: false,
    })

    return (
        <>
            {!isUploading ? (
                <div className='uploadBox' {...getRootProps()}>
                    <input {...getInputProps()} />
                    <FontAwesomeIcon icon={faUpload} className='uploadIcon' />
                    <h3>{isDragActive ? 'Drop files here...' : 'Upload your files'}</h3>
                    {error && <p className="errorText">{error}</p>}
                </div>
            ) : (
                <div className='uploadBox'>
                    <PulseLoader
                        className='loadingIcon'
                        color={'white'}
                        speedMultiplier={0.25}
                        loading={isUploading}
                        aria-label="Loading Spinner"
                        data-testid="loader"
                    />
                    <p>{progress}%</p>
                </div>
            )}
            {downloadLink !== null ? (
                <div className='linkBox'>
                    {downloadLink && (
                        <p className="downloadText">
                            Your one-time link: <a href={downloadLink}>{downloadLink}</a>
                        </p>
                    )}
                </div>
            ) : (<></>)}
        </>
    )
}

export default UploadBox
