import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faUpload } from '@fortawesome/free-solid-svg-icons'
import { useState, useEffect } from 'react'
import { PulseLoader } from "react-spinners";
import { useDropzone } from 'react-dropzone'

function UploadBox() {

    const [isUploading, setIsUploading] = useState(false)

    const handleUploadClick = () => {
        setIsUploading(true)
        console.log("Upload box clicked")
    }

    const onDrop = (acceptedFiles) => {
        setIsUploading(true)
        console.log("Files dropped:", acceptedFiles)
        // TODO: upload files to your backend
    }

    const { getRootProps, getInputProps, isDragActive } = useDropzone({
        onDrop,
        multiple: false, // allow multiple files
        // accept: { 'image/*': [] }, // optional: restrict file types
    })

    return (
        <>
            {!isUploading ? (
                <div className='uploadBox' {...getRootProps()}>
                    <input {...getInputProps()} />
                    <FontAwesomeIcon icon={faUpload} className='uploadIcon' />
                    <h3>{isDragActive ? 'Drop files here...' : 'Upload your files'}</h3>
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
                </div>
            )}
        </>
    )
}
export default UploadBox