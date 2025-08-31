export default {
    //preset: 'ts-jest',
    testEnvironment: 'jest-fixed-jsdom',
    testEnvironmentOptions: {
        customExportConditions: [''],
    },
    transform: {
        "^.+\\.tsx?$": "ts-jest"
    },
    setupFilesAfterEnv: ["@testing-library/jest-dom", "<rootDir>/src/setupTests.ts"],
    testTimeout: 10000,
    moduleNameMapper: {
        '\\.(gif|ttf|eot|svg|png)$': '<rootDir>/test/__mocks__/fileMock.js',
        '\\.(css|less|sass|scss)$': 'identity-obj-proxy',
    },
    collectCoverageFrom: [
        "src/**/*.{js,jsx,ts,tsx}",
        "!<rootDir>/node_modules/",
        "!src/setup-tests.ts"
    ],
    coverageReporters: [
        "text",
        "lcov"
    ],
    globals: {
        // Suppress React act() warnings from third-party components
        IS_REACT_ACT_ENVIRONMENT: true
    }
}