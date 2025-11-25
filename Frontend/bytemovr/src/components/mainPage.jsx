import "../assets/main.css"
import '../App.css'
import UploadBox from './uploadBox.jsx';
import { useLocation, useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import { googleLogout } from '@react-oauth/google';

function MainPage() {

    const location = useLocation();
    const navigate = useNavigate();
    const user = location.state?.user;

    useEffect(() => {
        if (!user) {
            navigate('/', { replace: true });
        }
    }, [user, navigate]);

    if (!user) {
        return null;
    }

    const handleLogout = () => {
        googleLogout();
        navigate('/', { replace: true });
    }

    return (
        <>
            <div>
                <header className='headerBox'>
                    <p className='logo'>ByteMovr</p>
                    <div className="welcomeMessageBox">
                        <p>{user ? `Welcome, ${user.name}` : ''}</p>
                    </div>
                </header>
                <main className="mainBox">
                    <UploadBox></UploadBox>
                </main>
                <footer className='footerBox'>
                    <p>© 2025 ByteMovr. All rights reserved.</p>
                    <button className='logoutButton' onClick={handleLogout}>Logout</button>
                </footer>
            </div>
        </>
    )
}

export default MainPage
