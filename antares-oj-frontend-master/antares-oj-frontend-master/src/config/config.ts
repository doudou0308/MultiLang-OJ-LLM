export const BACKEND_URL = 'http://localhost:8080'

export const HOME_URL = 'http://localhost:8002'

export const DEFAULT_AVATAR =
  'https://gw.alipayobjects.com/zos/antfincdn/XAosXuNZyF/BiazfanxmamNRoxxVxka.png'

export const LANGUAGES = [
  { name: 'Java', enabled: true },
  { name: 'C', enabled: true },
  { name: 'C++', enabled: false },
  { name: 'Python', enabled: false },
  { name: 'Go', enabled: false },
  { name: 'JavaScript', enabled: false },
]

export const CODE_TEMPLATES: Record<string, string> = {
  Java: `public class Main {
    public static void main(String[] args) {

    }
}`,
  C: `#include <stdio.h>

int main() {

    return 0;
}`,
}

export const CODE_TEMPLATE = CODE_TEMPLATES.Java
