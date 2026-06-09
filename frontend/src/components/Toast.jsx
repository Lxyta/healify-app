import { useState, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';

/**
 * Toast 通知组件 — 顶部滑入，自动消失
 *
 * 用法：
 *   const { toast, showToast } = useToast();
 *   showToast('✅ 保存成功！', 'success');   // success | error | info
 */
const TOAST_COLORS = {
  success: 'bg-green-600 text-white',
  error: 'bg-red-500 text-white',
  info: 'bg-blue-500 text-white',
};

const TOAST_ICONS = {
  success: '✅',
  error: '❌',
  info: 'ℹ️',
};

export default function Toast({ message, type = 'success', onClose }) {
  useEffect(() => {
    const timer = setTimeout(onClose, 3000);
    return () => clearTimeout(timer);
  }, [onClose]);

  return createPortal(
    <div className="fixed top-4 left-1/2 -translate-x-1/2 z-[9999] animate-slide-down">
      <div
        className={`flex items-center gap-2 px-5 py-3 rounded-xl shadow-lg text-sm font-medium
          cursor-pointer transition-all duration-200 hover:scale-105 ${TOAST_COLORS[type] || TOAST_COLORS.info}`}
        onClick={onClose}
      >
        <span className="text-base">{TOAST_ICONS[type] || TOAST_ICONS.info}</span>
        <span>{message}</span>
        <button className="ml-2 opacity-70 hover:opacity-100 text-lg leading-none">&times;</button>
      </div>
    </div>,
    document.body
  );
}

// ------ useToast hook ------

export function useToast() {
  const [toast, setToast] = useState(null);

  const showToast = useCallback((message, type = 'success') => {
    setToast({ message, type, key: Date.now() });
  }, []);

  const closeToast = useCallback(() => setToast(null), []);

  const ToastElement = toast ? (
    <Toast key={toast.key} message={toast.message} type={toast.type} onClose={closeToast} />
  ) : null;

  return { toast: ToastElement, showToast };
}
