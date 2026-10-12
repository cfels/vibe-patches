{
  pkgs ? import <nixpkgs> { },
}:

(pkgs.buildFHSEnv {
  name = "vibe-patches-env";
  targetPkgs =
    pkgs: with pkgs; [
      jdk21
      android-tools
      scrcpy
      python3
      curl
      unzip
      stdenv.cc.cc
      zlib
      libx11
      libxext
      libxdamage
      libxfixes
      libxcb
      libxcb-cursor
      libxcb-wm
      libxcb-image
      libxcb-keysyms
      libxcb-render-util
      libxcomposite
      libxcursor
      libxi
      libxrender
      libxtst
      libice
      libsm
      libxkbfile
      libxshmfence
      libpulseaudio
      libuuid
      libbsd
      libdrm
      expat
      freetype
      fontconfig
      glib
      libpng
      libjpeg_turbo
      libtiff
      libxkbcommon
      nss
      nspr
      alsa-lib
      libgbm
      libGL
      dbus
      systemd
      xkeyboard_config
    ];
  runScript = "bash";
  profile = ''
    export JAVA_HOME=${pkgs.jdk21}
    export ANDROID_HOME="''${ANDROID_HOME:-$HOME/Android/Sdk}"
    export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
    export QT_XKB_CONFIG_ROOT=${pkgs.xkeyboard_config}/share/X11/xkb
    export QTCOMPOSE=${pkgs.libx11}/share/X11/locale
  '';
}).env
