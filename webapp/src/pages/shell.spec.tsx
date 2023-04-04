import * as React from 'react'
import {render, screen} from '@testing-library/react'
import Shell from "./shell";

describe('Shell', () => {
    it('render shell component', () => {
        render(<Shell />);
    })
})