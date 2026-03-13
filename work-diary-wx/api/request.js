import Toast from 'tdesign-miniprogram/toast/index';

// 本地开发环境的后端地址

const BASE_URL = 'https://47.104.134.92';
// const BASE_URL = 'https://www.suntool.online';
// const BASE_URL = 'https://4vhdg4845791.vicp.fun';
/**
 * 封装微信 request 为 Promise 接口
 * 自动携带 Sa-Token (Authorization)
 * 自动处理 Result<T> 解包与全局异常拦截
 */
export const request = (options) => {
  return new Promise((resolve, reject) => {
    // 获取缓存中的 Token
    const token = wx.getStorageSync('Authorization');
    const header = options.header || {};

    // 如果有 Token，按 Sa-Token 默认要求的字段塞入 Header
    if (token) {
      header['workDiaryAuthorization'] = token;
    }

    // 默认 Content-Type
    if (!header['content-type']) {
      header['content-type'] = 'application/json';
    }

    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data,
      header: header,
      success: (res) => {
        const { statusCode, data } = res;

        // 1. HTTP 状态码非 200 (例如 404, 500 等网关或服务器宕机错误)
        if (statusCode !== 200) {
          Toast({ context: this, selector: '#t-toast', message: '网络请求异常，请稍后再试' });
          return reject(res);
        }

        // 2. HTTP 200，但是后端封装的 Result code 不为 200 ( com.workdiary.common.api.Result )
        if (data && data.code !== 200) {

          // 若后端抛出的是未登录 (401)
          if (data.code === 401) {
            wx.showToast({
              title: '登录已过期，请重新登录',
              icon: 'none',
              duration: 2000
            });
            wx.removeStorageSync('Authorization');
            // TODO: 可以按需控制跳转到授权页或我的页面
            setTimeout(() => {
              wx.switchTab({ url: '/pages/my/index' });
            }, 1000);
            return reject(data);
          }

          // 其他业务异常 (403, 404校验失败, 500 等)，直接 Toast 提示 message
          Toast({
            context: this,
            selector: '#t-toast',
            message: data.message || '系统繁忙',
            icon: 'error'
          });
          return reject(data);
        }

        // 3. 一切正常，只把后端的 data.data 抛给前端页面去用，不要外面那一层包了
        resolve(data.data);
      },
      fail: (err) => {
        Toast({
          context: this,
          selector: '#t-toast',
          message: '服务器连接失败，请检查网络',
          icon: 'error'
        });
        reject(err);
      }
    });
  });
};

// 工具导出，方便页面按需引入
export const get = (url, data, header) => request({ url, method: 'GET', data, header });
export const post = (url, data, header) => request({ url, method: 'POST', data, header });
export const put = (url, data, header) => request({ url, method: 'PUT', data, header });
export const del = (url, data, header) => request({ url, method: 'DELETE', data, header });

export const doLogin = () => {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (res) => {
        if (res.code) {
          wx.request({
            url: BASE_URL + '/wx/login',
            method: 'POST',
            data: { code: res.code },
            header: { 'content-type': 'application/json' },
            success: (loginRes) => {
              if (loginRes.data && loginRes.data.code === 200) {
                const tokenValue = loginRes.data.data.tokenValue;
                wx.setStorageSync('Authorization', tokenValue);
                resolve(tokenValue);
              } else {
                reject('Backend login failed');
              }
            },
            fail: reject
          });
        } else {
          reject('No code');
        }
      },
      fail: reject
    });
  });
};

/**
 * 将后端返回的文件路径转成可直接用于 <image src> 的完整 URL。
 *
 * - 私有桶代理路径（如 /file/download?key=20240101/abc.jpg）→ 拼接 BASE_URL 前缀
 * - 已是完整 URL（http/https 开头）→ 直接返回，不做处理
 *
 * 用法：
 *   <image src="{{ imgUrl }}" />
 *   this.setData({ imgUrl: getImageUrl(item.imageUrl) })
 */
export const getImageUrl = (path) => {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://')) {
    return path; // 公开桶直链或 CDN 地址，直接用
  }
  // 私有桶代理路径，拼接后端 baseUrl
  // path 形如 /file/download?key=20240101/uuid.jpg
  return BASE_URL + path;
};

/**
 * 封装微信 uploadFile，用于上传商单截图等文件
 * 自动携带 Sa-Token，返回 Promise<string>（上传后的文件访问路径）
 *
 * @param {string} filePath 本地文件路径（wx.chooseImage 返回的 tempFilePath）
 * @returns {Promise<string>} 服务端返回的文件代理路径
 */
export const uploadFile = (filePath) => {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('Authorization');
    wx.uploadFile({
      url: BASE_URL + '/file/upload',
      filePath,
      name: 'file',
      header: token ? { workDiaryAuthorization: token } : {},
      success: (res) => {
        const data = JSON.parse(res.data);
        if (data && data.code === 200) {
          resolve(data.data); // 返回代理路径，如 /file/download?key=xxx
        } else {
          reject(data?.message || '上传失败');
        }
      },
      fail: reject,
    });
  });
};

export default {
  request,
  get,
  post,
  put,
  del,
  doLogin,
  getImageUrl,
  uploadFile,
  BASE_URL,
};
