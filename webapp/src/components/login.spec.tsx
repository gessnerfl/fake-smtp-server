import React from "react";
import { screen, waitFor } from "@testing-library/react";
import { jest } from '@jest/globals';

const mockUseGetMetaDataQuery = jest.fn();
const mockUseLoginMutation = jest.fn();

jest.unstable_mockModule("../store/rest-api", () => {
  return {
    useGetMetaDataQuery: mockUseGetMetaDataQuery,
    useLoginMutation: mockUseLoginMutation
  };
});

const { renderWithProviders } = await import("../test-utils");
const { default: Login } = await import("./login");

describe("Login Component", () => {
  beforeEach(() => {
    jest.clearAllMocks();

    (mockUseGetMetaDataQuery as jest.Mock).mockReturnValue({
      data: {version: "test", authenticationEnabled: true},
      isLoading: false
    });

    const mockUnwrap = jest.fn<() => Promise<unknown>>().mockResolvedValue({});
    const mockLogin = jest.fn().mockReturnValue({unwrap: mockUnwrap});

    (mockUseLoginMutation as jest.Mock).mockReturnValue([
      mockLogin,
      {isLoading: false}
    ]);
  });

  test("does not render when authentication is disabled", async () => {
    (mockUseGetMetaDataQuery as jest.Mock).mockReturnValue({
      data: {version: "test", authenticationEnabled: false},
      isLoading: false
    });

    const {container} = renderWithProviders(<Login/>);

    await waitFor(() => {
      expect(container.firstChild).toBeNull();
    });
  });

  test("shows loading state when fetching metadata", async () => {
    (mockUseGetMetaDataQuery as jest.Mock).mockReturnValue({
      data: undefined,
      isLoading: true
    });

    renderWithProviders(<Login/>);

    expect(screen.getByText("Loading...")).toBeInTheDocument();
  });

  test("renders login form when authentication is enabled", async () => {
    renderWithProviders(<Login/>);

    await waitFor(() => {
      expect(screen.getByText("Login to FakeSMTP Server")).toBeInTheDocument();
      expect(screen.getByText("Username")).toBeInTheDocument();
      expect(screen.getByText("Password")).toBeInTheDocument();
      expect(screen.getByRole("button", {name: "Sign In"})).toBeInTheDocument();
    });
  });

  test("login button shows loading state during login", async () => {
    const mockUnwrap = jest.fn<() => Promise<unknown>>().mockImplementation(() => new Promise(resolve => {
      setTimeout(resolve, 10000);
    }));
    const mockLogin = jest.fn().mockReturnValue({unwrap: mockUnwrap});

    (mockUseLoginMutation as jest.Mock).mockReturnValue([
      mockLogin,
      {isLoading: true}
    ]);

    renderWithProviders(<Login/>);

    await waitFor(() => {
      expect(screen.getByText("Login to FakeSMTP Server")).toBeInTheDocument();
    });

    expect(screen.getByRole("button", {name: "Signing In..."})).toBeInTheDocument();
    expect(screen.getByRole("button", {name: "Signing In..."})).toBeDisabled();
  });
});
