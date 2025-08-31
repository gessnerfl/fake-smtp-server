import React from "react";
import { screen, waitFor } from "@testing-library/react";
import { renderWithProviders } from "../test-utils";
import Login from "./login";
import { useGetMetaDataQuery, useLoginMutation } from "../store/rest-api";

jest.mock("../store/rest-api", () => {
  return {
    useGetMetaDataQuery: jest.fn(),
    useLoginMutation: jest.fn()
  };
});

describe("Login Component", () => {
  beforeEach(() => {
    jest.clearAllMocks();

    (useGetMetaDataQuery as jest.Mock).mockReturnValue({
      data: {version: "test", authenticationEnabled: true},
      isLoading: false
    });

    const mockUnwrap = jest.fn().mockResolvedValue({});
    const mockLogin = jest.fn().mockReturnValue({unwrap: mockUnwrap});

    (useLoginMutation as jest.Mock).mockReturnValue([
      mockLogin,
      {isLoading: false}
    ]);
  });

  test("does not render when authentication is disabled", async () => {
    (useGetMetaDataQuery as jest.Mock).mockReturnValue({
      data: {version: "test", authenticationEnabled: false},
      isLoading: false
    });

    const {container} = renderWithProviders(<Login/>);

    await waitFor(() => {
      expect(container.firstChild).toBeNull();
    });
  });

  test("shows loading state when fetching metadata", async () => {
    (useGetMetaDataQuery as jest.Mock).mockReturnValue({
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
    const mockUnwrap = jest.fn().mockImplementation(() => new Promise(resolve => {
      setTimeout(resolve, 10000);
    }));
    const mockLogin = jest.fn().mockReturnValue({unwrap: mockUnwrap});

    (useLoginMutation as jest.Mock).mockReturnValue([
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
