import { useState } from 'react'
import reactLogo from './assets/react.svg'
import viteLogo from '/vite.svg'
import "./assets/main.css"
import './App.css'
import UploadBox from './components/uploadBox.jsx'

function App() {

  const [downloadLink, setDownloadLink] = useState("")

  return (
    <>
      <div>
        <header className='headerBox'>
          <p className='logo'>ByteMovr</p>
        </header>
        <main className="mainBox">
          <UploadBox></UploadBox>
        </main>
        <footer className='footerBox'>
          <p>© 2025 ByteMovr. All rights reserved.</p>
        </footer>
      </div>
    </>
  )
}

export default App
