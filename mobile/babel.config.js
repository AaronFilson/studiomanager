// Jest transforms with this; Metro would use babel-preset-expo without it.
module.exports = function (api) {
  api.cache(true);
  return { presets: ['babel-preset-expo'] };
};
