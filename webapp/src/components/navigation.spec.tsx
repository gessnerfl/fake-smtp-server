import * as React from 'react'
import '@testing-library/jest-dom'
import {render, screen} from '@testing-library/react'
import Navigation from "./navigation";

describe('Navigation', () => {
    it('render navigation component', () => {
        render(<Navigation />);
        expect(screen.getByText("FakeSMTPServer")).toBeInTheDocument()
    })
})