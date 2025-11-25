import { useState } from 'react'
import "../assets/main.css"
import '../App.css'
import UploadBox from './uploadBox';
import { GoogleLogin, googleLogout, useGoogleLogin } from '@react-oauth/google';
import { jwtDecode } from 'jwt-decode';
import { useNavigate } from 'react-router-dom';

function LoginPage() {

  const [downloadLink, setDownloadLink] = useState("")

  const navigate = useNavigate();

  const handleLogin = (credentialResponse) => {
    navigate('/app', { state: { user: jwtDecode(credentialResponse.credential) } });
  }

  const handleError = () => {
    navigate('/')
  }

  return (
    <>
      <div>
        <header className='headerBox'>
          <p className='logo'>ByteMovr</p>
        </header>
        <main className="mainBox">
          <h1>ByteMovr</h1>
          <p>The ultimate file transfer solution.</p>
          <GoogleLogin
            onSuccess={(credentialResponse) => handleLogin(credentialResponse)}
            onError={() => handleError()}
            auto_select={true}
          />
        </main>
        <footer className='footerBox'>
          <p>© 2025 ByteMovr. All rights reserved.</p>
        </footer>
      </div>
    </>
  )
}

export default LoginPage
