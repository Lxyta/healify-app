import { Link, useLocation, useNavigate } from 'react-router-dom';

const NAV_ITEMS = [
  { path: '/', label: '首页', icon: '🏠' },
  { path: '/profile', label: '健康档案', icon: '📋' },
  { path: '/plan', label: '我的计划', icon: '📅' },
  { path: '/history', label: '历史记录', icon: '📊' },
];

export default function Navbar() {
  const location = useLocation();
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem('user') || '{}');

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <nav className="bg-white shadow-sm border-b border-gray-100 sticky top-0 z-50">
      <div className="max-w-4xl mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2 text-healify-700 font-bold text-xl">
            <span>💚</span>
            <span>Healify</span>
          </Link>

          {/* Nav Links */}
          <div className="hidden sm:flex items-center gap-1">
            {NAV_ITEMS.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                  location.pathname === item.path
                    ? 'bg-healify-50 text-healify-700'
                    : 'text-gray-600 hover:text-healify-600 hover:bg-healify-50'
                }`}
              >
                <span className="mr-1.5">{item.icon}</span>
                {item.label}
              </Link>
            ))}
          </div>

          {/* User */}
          <div className="flex items-center gap-3">
            <span className="text-sm text-gray-600 hidden sm:inline">
              👤 {user.nickname || user.username}
            </span>
            <button onClick={handleLogout} className="text-sm text-gray-400 hover:text-red-500 transition-colors">
              退出
            </button>
          </div>
        </div>

        {/* Mobile Nav */}
        <div className="sm:hidden flex justify-around pb-2">
          {NAV_ITEMS.map((item) => (
            <Link
              key={item.path}
              to={item.path}
              className={`flex flex-col items-center text-xs gap-0.5 px-2 py-1 rounded ${
                location.pathname === item.path ? 'text-healify-600' : 'text-gray-500'
              }`}
            >
              <span className="text-lg">{item.icon}</span>
              {item.label}
            </Link>
          ))}
        </div>
      </div>
    </nav>
  );
}
