{ pkgs }: {
  deps = [
    pkgs.jdk21
    pkgs.maven
    pkgs.nodejs_22
    pkgs.nodePackages.pnpm
  ];
}

