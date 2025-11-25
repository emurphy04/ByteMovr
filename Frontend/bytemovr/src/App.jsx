import "./assets/main.css"
import './App.css'
import LoginPage from './components/loginPage.jsx'
import MainPage from './components/mainPage.jsx'
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';


function App() {

  return (
    <Router>
      <Routes>
        <Route path="/" element={<LoginPage />} />
        <Route path="/app" element={<MainPage />} />
      </Routes>
    </Router>
  )
}

export default App
